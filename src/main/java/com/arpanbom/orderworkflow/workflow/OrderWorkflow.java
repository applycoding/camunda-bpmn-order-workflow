package com.arpanbom.orderworkflow.workflow;

import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.api.StartOrderRequest;
import com.arpanbom.orderworkflow.api.StartOrderResponse;
import io.camunda.client.api.command.ClientException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderWorkflow {

    private final ProcessStarter processes;

    public OrderWorkflow(ProcessStarter processes) {
        this.processes = processes;
    }

    public StartOrderResponse start(StartOrderRequest request) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(OrderProcess.VAR_ORDER_ID, request.orderId());
        variables.put(OrderProcess.VAR_CUSTOMER_ID, request.customerId());
        variables.put(OrderProcess.VAR_AMOUNT, request.amount().doubleValue());
        variables.put(OrderProcess.VAR_CURRENCY, request.currency());
        try {
            ProcessStarter.StartedProcess started = processes.start(OrderProcess.BPMN_PROCESS_ID, variables);
            return new StartOrderResponse(
                    request.orderId(),
                    Long.toString(started.processInstanceKey()),
                    started.bpmnProcessId());
        } catch (ClientException ex) {
            throw new WorkflowUnavailableException(
                    "The workflow engine did not accept the order. Start Zeebe and try again.", ex);
        }
    }
}
