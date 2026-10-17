package com.arpanbom.orderworkflow.fake;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class FakeInventoryService implements InventoryService {

    private final ConcurrentHashMap<String, String> reservations = new ConcurrentHashMap<>();

    @Override
    public String reserve(String orderId) {
        String reservationId = "res-" + orderId;
        reservations.put(orderId, reservationId);
        return reservationId;
    }

    @Override
    public void release(String orderId, String reservationId) {
        String held = reservations.get(orderId);
        if (held == null || !held.equals(reservationId)) {
            throw new IllegalStateException("No reservation " + reservationId + " for order " + orderId + ".");
        }
        reservations.remove(orderId);
    }
}
