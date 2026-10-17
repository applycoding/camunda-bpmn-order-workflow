package com.arpanbom.orderworkflow.fake;

import java.math.BigDecimal;

public interface PaymentGateway {

    String charge(String orderId, BigDecimal amount, String currency);

    String refund(String orderId, String paymentId);
}
