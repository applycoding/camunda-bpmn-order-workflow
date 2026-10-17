package com.arpanbom.orderworkflow.process;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.arpanbom.orderworkflow.OrderProcess;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@ZeebeProcessTest
@Timeout(value = 3, unit = TimeUnit.MINUTES)
class OrderApprovalProcessTest {

    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration APPROVAL_SLA = Duration.ofMinutes(15);

    private ZeebeTestEngine engine;
    private ZeebeClient client;

    @BeforeEach
    void deployModels() throws InterruptedException, TimeoutException {
        var deployment = client.newDeployResourceCommand()
                .addResourceFromClasspath("bpmn/order-approval.bpmn")
                .addResourceFromClasspath("dmn/approval-threshold.dmn")
                .send()
                .join();
        BpmnAssert.assertThat(deployment).containsProcessesByBpmnProcessId(OrderProcess.BPMN_PROCESS_ID);
        waitForIdle();
    }

    @Test
    void completesBelowTheThresholdWithoutManualApproval() throws Exception {
        ProcessInstanceEvent instance = start("ord-100", "cust-9", 1000);

        complete(OrderProcess.CHECK_CREDIT, Map.of(
                OrderProcess.VAR_CREDIT_APPROVED, true,
                OrderProcess.VAR_CREDIT_SCORE, 760,
                OrderProcess.VAR_CREDIT_REASON, "approved"));
        complete(OrderProcess.RESERVE_INVENTORY, Map.of(
                OrderProcess.VAR_RESERVATION_ID, "res-ord-100",
                OrderProcess.VAR_INVENTORY_STATUS, OrderProcess.STATUS_RESERVED));
        complete(OrderProcess.COLLECT_PAYMENT, Map.of(
                OrderProcess.VAR_PAYMENT_ID, "pay-ord-100",
                OrderProcess.VAR_PAYMENT_STATUS, OrderProcess.STATUS_COLLECTED));
        complete(OrderProcess.FULFIL_ORDER, Map.of(
                OrderProcess.VAR_SHIPMENT_ID, "shp-ord-100",
                OrderProcess.VAR_FULFILMENT_STATUS, OrderProcess.STATUS_SHIPPED));

        BpmnAssert.assertThat(instance)
                .hasNoIncidents()
                .hasPassedElementsInOrder(
                        OrderProcess.CHECK_CREDIT,
                        OrderProcess.DECIDE_APPROVAL,
                        OrderProcess.RESERVE_INVENTORY,
                        OrderProcess.COLLECT_PAYMENT,
                        OrderProcess.FULFIL_ORDER,
                        OrderProcess.END_COMPLETED)
                .hasVariableWithValue(OrderProcess.VAR_REQUIRES_MANUAL_APPROVAL, false)
                .hasVariableWithValue(OrderProcess.VAR_PAYMENT_ID, "pay-ord-100")
                .hasNotPassedElement(OrderProcess.APPROVE_ORDER)
                .hasNotPassedElement(OrderProcess.ESCALATE_APPROVAL)
                .hasNotPassedElement(OrderProcess.REFUND_PAYMENT)
                .hasNotPassedElement(OrderProcess.RELEASE_INVENTORY)
                .isCompleted();
    }

    @Test
    void rejectsWhenCreditIsDeclined() throws Exception {
        ProcessInstanceEvent instance = start("ord-blocked", "blocked-cust", 1500);

        complete(OrderProcess.CHECK_CREDIT, Map.of(
                OrderProcess.VAR_CREDIT_APPROVED, false,
                OrderProcess.VAR_CREDIT_SCORE, 410,
                OrderProcess.VAR_CREDIT_REASON, "declined"));

        BpmnAssert.assertThat(instance)
                .hasNoIncidents()
                .hasPassedElement(OrderProcess.END_CREDIT_REJECTED)
                .hasNotPassedElement(OrderProcess.DECIDE_APPROVAL)
                .hasNotPassedElement(OrderProcess.COLLECT_PAYMENT)
                .isCompleted();
    }

    @Test
    void rejectsWhenTheApproverDeclines() throws Exception {
        ProcessInstanceEvent instance = start("ord-review", "cust-9", 1500);

        complete(OrderProcess.CHECK_CREDIT, Map.of(
                OrderProcess.VAR_CREDIT_APPROVED, true,
                OrderProcess.VAR_CREDIT_SCORE, 760,
                OrderProcess.VAR_CREDIT_REASON, "approved"));

        BpmnAssert.assertThat(instance)
                .isWaitingAtElements(OrderProcess.APPROVE_ORDER)
                .hasVariableWithValue(OrderProcess.VAR_REQUIRES_MANUAL_APPROVAL, true);

        completeUserTask(false);

        BpmnAssert.assertThat(instance)
                .hasNoIncidents()
                .hasPassedElement(OrderProcess.END_REJECTED)
                .hasNotPassedElement(OrderProcess.RESERVE_INVENTORY)
                .hasNotPassedElement(OrderProcess.COLLECT_PAYMENT)
                .isCompleted();
    }

    @Test
    void paymentFailureReleasesTheReservation() throws Exception {
        ProcessInstanceEvent instance = start("payfail-ord", "cust-9", 200);

        complete(OrderProcess.CHECK_CREDIT, Map.of(
                OrderProcess.VAR_CREDIT_APPROVED, true,
                OrderProcess.VAR_CREDIT_SCORE, 760,
                OrderProcess.VAR_CREDIT_REASON, "approved"));
        complete(OrderProcess.RESERVE_INVENTORY, Map.of(
                OrderProcess.VAR_RESERVATION_ID, "res-payfail-ord",
                OrderProcess.VAR_INVENTORY_STATUS, OrderProcess.STATUS_RESERVED));
        throwBpmnError(OrderProcess.COLLECT_PAYMENT, OrderProcess.PAYMENT_FAILED, "Demo gateway declined the charge.");

        assertNull(tryActivate(OrderProcess.REFUND_PAYMENT), "A declined charge must not be refunded.");
        complete(OrderProcess.RELEASE_INVENTORY, Map.of(
                OrderProcess.VAR_INVENTORY_STATUS, OrderProcess.STATUS_RELEASED));

        BpmnAssert.assertThat(instance)
                .hasNoIncidents()
                .hasPassedElement(OrderProcess.RELEASE_INVENTORY)
                .hasPassedElement(OrderProcess.END_PAYMENT_FAILED)
                .hasNotPassedElement(OrderProcess.REFUND_PAYMENT)
                .hasNotPassedElement(OrderProcess.FULFIL_ORDER)
                .isCompleted();
    }

    @Test
    void fulfilmentFailureRefundsThePaymentAndReleasesStock() throws Exception {
        ProcessInstanceEvent instance = start("shipfail-ord", "cust-9", 200);

        complete(OrderProcess.CHECK_CREDIT, Map.of(
                OrderProcess.VAR_CREDIT_APPROVED, true,
                OrderProcess.VAR_CREDIT_SCORE, 760,
                OrderProcess.VAR_CREDIT_REASON, "approved"));
        complete(OrderProcess.RESERVE_INVENTORY, Map.of(
                OrderProcess.VAR_RESERVATION_ID, "res-shipfail-ord",
                OrderProcess.VAR_INVENTORY_STATUS, OrderProcess.STATUS_RESERVED));
        complete(OrderProcess.COLLECT_PAYMENT, Map.of(
                OrderProcess.VAR_PAYMENT_ID, "pay-shipfail-ord",
                OrderProcess.VAR_PAYMENT_STATUS, OrderProcess.STATUS_COLLECTED));
        throwBpmnError(OrderProcess.FULFIL_ORDER, OrderProcess.FULFILMENT_FAILED, "Demo warehouse could not ship.");

        completeAvailable(
                OrderProcess.REFUND_PAYMENT,
                OrderProcess.RELEASE_INVENTORY);

        BpmnAssert.assertThat(instance)
                .hasNoIncidents()
                .hasPassedElement(OrderProcess.COLLECT_PAYMENT)
                .hasPassedElement(OrderProcess.REFUND_PAYMENT)
                .hasPassedElement(OrderProcess.RELEASE_INVENTORY)
                .hasPassedElement(OrderProcess.END_FULFILMENT_FAILED)
                .isCompleted();
    }

    @Test
    void escalatesAWaitingApprovalAndStillAllowsItToComplete() throws Exception {
        ProcessInstanceEvent instance = start("ord-slow", "cust-9", 1500);

        complete(OrderProcess.CHECK_CREDIT, Map.of(
                OrderProcess.VAR_CREDIT_APPROVED, true,
                OrderProcess.VAR_CREDIT_SCORE, 760,
                OrderProcess.VAR_CREDIT_REASON, "approved"));

        BpmnAssert.assertThat(instance).isWaitingExactlyAtElements(OrderProcess.APPROVE_ORDER);

        increaseTime(APPROVAL_SLA.plusMillis(1));
        complete(OrderProcess.ESCALATE_APPROVAL, Map.of("escalated", true));

        BpmnAssert.assertThat(instance)
                .hasNoIncidents()
                .hasPassedElement(OrderProcess.ESCALATE_APPROVAL)
                .hasPassedElement(OrderProcess.END_ESCALATION_NOTIFIED)
                .isWaitingExactlyAtElements(OrderProcess.APPROVE_ORDER)
                .isNotCompleted();

        completeUserTask(true);
        complete(OrderProcess.RESERVE_INVENTORY, Map.of(
                OrderProcess.VAR_RESERVATION_ID, "res-ord-slow",
                OrderProcess.VAR_INVENTORY_STATUS, OrderProcess.STATUS_RESERVED));
        complete(OrderProcess.COLLECT_PAYMENT, Map.of(
                OrderProcess.VAR_PAYMENT_ID, "pay-ord-slow",
                OrderProcess.VAR_PAYMENT_STATUS, OrderProcess.STATUS_COLLECTED));
        complete(OrderProcess.FULFIL_ORDER, Map.of(
                OrderProcess.VAR_SHIPMENT_ID, "shp-ord-slow",
                OrderProcess.VAR_FULFILMENT_STATUS, OrderProcess.STATUS_SHIPPED));

        BpmnAssert.assertThat(instance)
                .hasNoIncidents()
                .hasPassedElement(OrderProcess.END_COMPLETED)
                .isCompleted();
    }

    private ProcessInstanceEvent start(String orderId, String customerId, double amount)
            throws InterruptedException, TimeoutException {
        ProcessInstanceEvent instance = client.newCreateInstanceCommand()
                .bpmnProcessId(OrderProcess.BPMN_PROCESS_ID)
                .latestVersion()
                .variables(Map.of(
                        OrderProcess.VAR_ORDER_ID, orderId,
                        OrderProcess.VAR_CUSTOMER_ID, customerId,
                        OrderProcess.VAR_AMOUNT, amount,
                        OrderProcess.VAR_CURRENCY, "EUR"))
                .send()
                .join();
        waitForIdle();
        return instance;
    }

    private void completeAvailable(String... jobTypes) throws InterruptedException, TimeoutException {
        Set<String> pending = new LinkedHashSet<>(List.of(jobTypes));
        List<String> handled = new ArrayList<>();
        for (int guard = 0; guard < 8 && !pending.isEmpty(); guard++) {
            boolean progressed = false;
            for (String jobType : new ArrayList<>(pending)) {
                ActivatedJob job = tryActivate(jobType);
                if (job == null) {
                    continue;
                }
                progressed = true;
                pending.remove(jobType);
                handled.add(jobType);
                Map<String, Object> variables = OrderProcess.REFUND_PAYMENT.equals(jobType)
                        ? Map.of(
                                OrderProcess.VAR_REFUND_ID, "refund-demo",
                                OrderProcess.VAR_PAYMENT_STATUS, OrderProcess.STATUS_REFUNDED)
                        : Map.of(OrderProcess.VAR_INVENTORY_STATUS, OrderProcess.STATUS_RELEASED);
                client.newCompleteCommand(job.getKey()).variables(variables).send().join();
                waitForIdle();
            }
            if (!progressed) {
                waitForIdle();
            }
        }
        if (!pending.isEmpty()) {
            fail("Compensation jobs still open: " + pending + ". Completed: " + handled);
        }
    }

    private void complete(String jobType, Map<String, Object> variables) throws InterruptedException, TimeoutException {
        ActivatedJob job = activate(jobType);
        client.newCompleteCommand(job.getKey()).variables(variables).send().join();
        waitForIdle();
    }

    private void completeUserTask(boolean approved) throws InterruptedException, TimeoutException {
        ActivatedJob job = activate(OrderProcess.USER_TASK_JOB_TYPE);
        if (!OrderProcess.APPROVE_ORDER.equals(job.getElementId())) {
            fail("Activated user task " + job.getElementId() + " instead of " + OrderProcess.APPROVE_ORDER);
        }
        client.newCompleteCommand(job.getKey())
                .variables(Map.of(
                        OrderProcess.VAR_APPROVED, approved,
                        OrderProcess.VAR_APPROVER, "a.bom"))
                .send()
                .join();
        waitForIdle();
    }

    private void throwBpmnError(String jobType, String errorCode, String message)
            throws InterruptedException, TimeoutException {
        ActivatedJob job = activate(jobType);
        client.newThrowErrorCommand(job.getKey())
                .errorCode(errorCode)
                .errorMessage(message)
                .send()
                .join();
        waitForIdle();
    }

    private ActivatedJob activate(String jobType) throws InterruptedException, TimeoutException {
        for (int attempt = 0; attempt < 8; attempt++) {
            ActivatedJob job = tryActivate(jobType);
            if (job != null) {
                return job;
            }
            waitForIdle();
        }
        fail("No activatable job of type " + jobType);
        return null;
    }

    private ActivatedJob tryActivate(String jobType) {
        var response = client.newActivateJobsCommand()
                .jobType(jobType)
                .maxJobsToActivate(1)
                .timeout(Duration.ofMinutes(2))
                .send()
                .join();
        if (response.getJobs().isEmpty()) {
            return null;
        }
        return response.getJobs().get(0);
    }

    private void waitForIdle() throws InterruptedException, TimeoutException {
        engine.waitForIdleState(IDLE_TIMEOUT);
    }

    private void increaseTime(Duration duration) throws InterruptedException, TimeoutException {
        waitForIdle();
        engine.increaseTime(duration);
        try {
            engine.waitForBusyState(Duration.ofSeconds(2));
            waitForIdle();
        } catch (TimeoutException ignored) {
            waitForIdle();
        }
    }
}
