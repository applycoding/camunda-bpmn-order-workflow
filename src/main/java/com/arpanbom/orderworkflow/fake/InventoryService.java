package com.arpanbom.orderworkflow.fake;

public interface InventoryService {

    String reserve(String orderId);

    void release(String orderId, String reservationId);
}
