package com.arpanbom.orderworkflow.fake;

public interface FulfilmentService {

    String ship(String orderId, String reservationId, String paymentId);
}
