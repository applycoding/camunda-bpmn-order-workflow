package com.arpanbom.orderworkflow.fake;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory payment stand-in. Order ids with the prefix {@code payfail-} are declined.
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

    public static final String DECLINE_PREFIX = "payfail-";

    private final ConcurrentHashMap<String, String> charges = new ConcurrentHashMap<>();

    @Override
    public String charge(String orderId, BigDecimal amount, String currency) {
        if (orderId != null && orderId.startsWith(DECLINE_PREFIX)) {
            throw new PaymentDeclinedException("Demo gateway declined the charge for " + orderId + ".");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentDeclinedException("Demo gateway declined a non-positive amount.");
        }
        String paymentId = "pay-" + orderId;
        charges.put(paymentId, orderId);
        return paymentId;
    }

    @Override
    public String refund(String orderId, String paymentId) {
        String chargedOrder = charges.remove(paymentId);
        if (chargedOrder == null || !chargedOrder.equals(orderId)) {
            throw new IllegalStateException("No collected payment " + paymentId + " for order " + orderId + ".");
        }
        return "refund-" + paymentId;
    }
}
