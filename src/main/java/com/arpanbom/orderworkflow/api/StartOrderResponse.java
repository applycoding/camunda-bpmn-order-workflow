package com.arpanbom.orderworkflow.api;

public record StartOrderResponse(String orderId, String processInstanceKey, String bpmnProcessId) {}
