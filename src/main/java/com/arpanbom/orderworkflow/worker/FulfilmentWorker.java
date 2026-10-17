package com.arpanbom.orderworkflow.worker;

import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.fake.FulfilmentFailedException;
import com.arpanbom.orderworkflow.fake.FulfilmentService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.exception.CamundaError;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FulfilmentWorker {

    private final FulfilmentService fulfilment;

    public FulfilmentWorker(FulfilmentService fulfilment) {
        this.fulfilment = fulfilment;
    }

    @JobWorker(type = OrderProcess.FULFIL_ORDER)
    public Map<String, Object> fulfil(
            @Variable(name = OrderProcess.VAR_ORDER_ID) String orderId,
            @Variable(name = OrderProcess.VAR_RESERVATION_ID) String reservationId,
            @Variable(name = OrderProcess.VAR_PAYMENT_ID) String paymentId) {
        try {
            String shipmentId = fulfilment.ship(orderId, reservationId, paymentId);
            return Map.of(
                    OrderProcess.VAR_SHIPMENT_ID, shipmentId,
                    OrderProcess.VAR_FULFILMENT_STATUS, OrderProcess.STATUS_SHIPPED);
        } catch (FulfilmentFailedException failed) {
            throw CamundaError.bpmnError(OrderProcess.FULFILMENT_FAILED, failed.getMessage());
        }
    }
}
