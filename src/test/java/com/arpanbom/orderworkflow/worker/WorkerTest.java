package com.arpanbom.orderworkflow.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.fake.FakeCreditBureau;
import com.arpanbom.orderworkflow.fake.FakeFulfilmentService;
import com.arpanbom.orderworkflow.fake.FakeInventoryService;
import com.arpanbom.orderworkflow.fake.FakePaymentGateway;
import com.arpanbom.orderworkflow.fake.InMemoryEscalationNotifier;
import io.camunda.client.exception.BpmnError;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkerTest {

    @Test
    void creditCheckApprovesANormalCustomer() {
        Map<String, Object> variables = new CreditCheckWorker(new FakeCreditBureau()).checkCredit("cust-9");

        assertEquals(true, variables.get(OrderProcess.VAR_CREDIT_APPROVED));
        assertEquals(FakeCreditBureau.APPROVED_SCORE, variables.get(OrderProcess.VAR_CREDIT_SCORE));
    }

    @Test
    void creditCheckDeclinesABlockedCustomer() {
        Map<String, Object> variables = new CreditCheckWorker(new FakeCreditBureau()).checkCredit("blocked-cust");

        assertEquals(false, variables.get(OrderProcess.VAR_CREDIT_APPROVED));
        assertEquals(FakeCreditBureau.DECLINED_SCORE, variables.get(OrderProcess.VAR_CREDIT_SCORE));
    }

    @Test
    void inventoryReserveCanBeReleased() {
        FakeInventoryService inventory = new FakeInventoryService();
        InventoryWorker worker = new InventoryWorker(inventory);

        Map<String, Object> reserved = worker.reserve("ord-1");
        assertEquals("res-ord-1", reserved.get(OrderProcess.VAR_RESERVATION_ID));
        assertEquals(OrderProcess.STATUS_RESERVED, reserved.get(OrderProcess.VAR_INVENTORY_STATUS));

        Map<String, Object> released = worker.release("ord-1", "res-ord-1");
        assertEquals(OrderProcess.STATUS_RELEASED, released.get(OrderProcess.VAR_INVENTORY_STATUS));
    }

    @Test
    void inventoryReleaseRejectsAnUnknownReservation() {
        InventoryWorker worker = new InventoryWorker(new FakeInventoryService());

        assertThrows(IllegalStateException.class, () -> worker.release("ord-1", "res-missing"));
    }

    @Test
    void paymentCollectsThenRefunds() {
        FakePaymentGateway gateway = new FakePaymentGateway();
        PaymentWorker worker = new PaymentWorker(gateway);

        Map<String, Object> collected = worker.collectPayment("ord-1", 40, "EUR");
        assertEquals("pay-ord-1", collected.get(OrderProcess.VAR_PAYMENT_ID));
        assertEquals(OrderProcess.STATUS_COLLECTED, collected.get(OrderProcess.VAR_PAYMENT_STATUS));

        Map<String, Object> refunded = worker.refundPayment("ord-1", "pay-ord-1");
        assertEquals("refund-pay-ord-1", refunded.get(OrderProcess.VAR_REFUND_ID));
        assertEquals(OrderProcess.STATUS_REFUNDED, refunded.get(OrderProcess.VAR_PAYMENT_STATUS));
    }

    @Test
    void paymentDeclineBecomesABpmnError() {
        PaymentWorker worker = new PaymentWorker(new FakePaymentGateway());

        BpmnError error = assertThrows(
                BpmnError.class,
                () -> worker.collectPayment("payfail-ord", new BigDecimal("15.00"), "EUR"));

        assertEquals(OrderProcess.PAYMENT_FAILED, error.getErrorCode());
        assertTrue(error.getErrorMessage().contains("payfail-ord"));
    }

    @Test
    void fulfilmentShipsACollectedOrder() {
        Map<String, Object> variables = new FulfilmentWorker(new FakeFulfilmentService())
                .fulfil("ord-1", "res-ord-1", "pay-ord-1");

        assertEquals("shp-ord-1", variables.get(OrderProcess.VAR_SHIPMENT_ID));
        assertEquals(OrderProcess.STATUS_SHIPPED, variables.get(OrderProcess.VAR_FULFILMENT_STATUS));
    }

    @Test
    void fulfilmentFailureBecomesABpmnError() {
        FulfilmentWorker worker = new FulfilmentWorker(new FakeFulfilmentService());

        BpmnError error = assertThrows(
                BpmnError.class,
                () -> worker.fulfil("shipfail-ord", "res-shipfail-ord", "pay-shipfail-ord"));

        assertEquals(OrderProcess.FULFILMENT_FAILED, error.getErrorCode());
    }

    @Test
    void escalationRecordsANotice() {
        InMemoryEscalationNotifier notifier = new InMemoryEscalationNotifier();
        new EscalationWorker(notifier).escalate("ord-9", 1500, "EUR");

        assertEquals(1, notifier.notices().size());
        assertEquals("ord-9", notifier.notices().get(0).orderId());
        assertEquals(0, notifier.notices().get(0).amount().compareTo(new BigDecimal("1500.0")));
        assertEquals("EUR", notifier.notices().get(0).currency());
    }
}
