package com.arpanbom.orderworkflow.fake;

import org.springframework.stereotype.Component;

/**
 * In-memory warehouse stand-in. Order ids with the prefix {@code shipfail-} cannot be shipped.
 */
@Component
public class FakeFulfilmentService implements FulfilmentService {

    public static final String FAILURE_PREFIX = "shipfail-";

    @Override
    public String ship(String orderId, String reservationId, String paymentId) {
        if (orderId != null && orderId.startsWith(FAILURE_PREFIX)) {
            throw new FulfilmentFailedException("Demo warehouse could not ship " + orderId + ".");
        }
        if (reservationId == null || reservationId.isBlank() || paymentId == null || paymentId.isBlank()) {
            throw new FulfilmentFailedException("Shipment for " + orderId + " is missing a reservation or payment.");
        }
        return "shp-" + orderId;
    }
}
