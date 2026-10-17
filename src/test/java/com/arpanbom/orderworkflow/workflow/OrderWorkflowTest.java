package com.arpanbom.orderworkflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.api.StartOrderRequest;
import com.arpanbom.orderworkflow.api.StartOrderResponse;
import io.camunda.client.api.command.ClientException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderWorkflowTest {

    @Test
    void startsTheOrderProcessWithNumericAmount() {
        CapturingStarter starter = new CapturingStarter(42L);
        OrderWorkflow workflow = new OrderWorkflow(starter);

        StartOrderResponse response = workflow.start(new StartOrderRequest(
                "ord-1", "cust-9", new BigDecimal("1250.50"), "EUR"));

        assertEquals("ord-1", response.orderId());
        assertEquals("42", response.processInstanceKey());
        assertEquals(OrderProcess.BPMN_PROCESS_ID, response.bpmnProcessId());
        assertEquals(OrderProcess.BPMN_PROCESS_ID, starter.bpmnProcessId);
        assertEquals("ord-1", starter.variables.get(OrderProcess.VAR_ORDER_ID));
        assertEquals("cust-9", starter.variables.get(OrderProcess.VAR_CUSTOMER_ID));
        assertEquals(1250.50d, starter.variables.get(OrderProcess.VAR_AMOUNT));
        assertEquals("EUR", starter.variables.get(OrderProcess.VAR_CURRENCY));
    }

    @Test
    void reportsWhenTheEngineRejectsTheStart() {
        OrderWorkflow workflow = new OrderWorkflow((processId, variables) -> {
            throw new ClientException("broker unavailable");
        });

        WorkflowUnavailableException error = assertThrows(
                WorkflowUnavailableException.class,
                () -> workflow.start(new StartOrderRequest("ord-1", "cust-9", new BigDecimal("10.00"), "EUR")));

        assertEquals("The workflow engine did not accept the order. Start Zeebe and try again.", error.getMessage());
    }

    private static final class CapturingStarter implements ProcessStarter {
        private final long key;
        private String bpmnProcessId;
        private Map<String, Object> variables;

        private CapturingStarter(long key) {
            this.key = key;
        }

        @Override
        public StartedProcess start(String bpmnProcessId, Map<String, Object> variables) {
            this.bpmnProcessId = bpmnProcessId;
            this.variables = variables;
            return new StartedProcess(key, bpmnProcessId);
        }
    }
}
