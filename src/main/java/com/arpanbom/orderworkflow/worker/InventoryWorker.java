package com.arpanbom.orderworkflow.worker;

import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.fake.InventoryService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InventoryWorker {

    private final InventoryService inventory;

    public InventoryWorker(InventoryService inventory) {
        this.inventory = inventory;
    }

    @JobWorker(type = OrderProcess.RESERVE_INVENTORY)
    public Map<String, Object> reserve(@Variable(name = OrderProcess.VAR_ORDER_ID) String orderId) {
        String reservationId = inventory.reserve(orderId);
        return Map.of(
                OrderProcess.VAR_RESERVATION_ID, reservationId,
                OrderProcess.VAR_INVENTORY_STATUS, OrderProcess.STATUS_RESERVED);
    }

    @JobWorker(type = OrderProcess.RELEASE_INVENTORY)
    public Map<String, Object> release(
            @Variable(name = OrderProcess.VAR_ORDER_ID) String orderId,
            @Variable(name = OrderProcess.VAR_RESERVATION_ID) String reservationId) {
        inventory.release(orderId, reservationId);
        return Map.of(OrderProcess.VAR_INVENTORY_STATUS, OrderProcess.STATUS_RELEASED);
    }
}
