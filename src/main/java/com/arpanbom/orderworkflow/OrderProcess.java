package com.arpanbom.orderworkflow;

/**
 * Names shared by the BPMN model, the DMN result, and the job workers.
 * The approval threshold itself lives in {@code dmn/approval-threshold.dmn}.
 */
public final class OrderProcess {

    public static final String BPMN_PROCESS_ID = "order-approval";

    public static final String CHECK_CREDIT = "check-credit";
    public static final String DECIDE_APPROVAL = "decide-approval";
    public static final String APPROVE_ORDER = "approve-order";
    public static final String ESCALATE_APPROVAL = "escalate-approval";
    public static final String RESERVE_INVENTORY = "reserve-inventory";
    public static final String RELEASE_INVENTORY = "release-inventory";
    public static final String COLLECT_PAYMENT = "collect-payment";
    public static final String REFUND_PAYMENT = "refund-payment";
    public static final String FULFIL_ORDER = "fulfil-order";

    public static final String USER_TASK_JOB_TYPE = "io.camunda.zeebe:userTask";

    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String FULFILMENT_FAILED = "FULFILMENT_FAILED";

    public static final String END_CREDIT_REJECTED = "end-credit-rejected";
    public static final String END_REJECTED = "end-rejected";
    public static final String END_PAYMENT_FAILED = "end-payment-failed";
    public static final String END_FULFILMENT_FAILED = "end-fulfilment-failed";
    public static final String END_COMPLETED = "end-completed";
    public static final String END_ESCALATION_NOTIFIED = "end-escalation-notified";

    public static final String VAR_ORDER_ID = "orderId";
    public static final String VAR_CUSTOMER_ID = "customerId";
    public static final String VAR_AMOUNT = "amount";
    public static final String VAR_CURRENCY = "currency";
    public static final String VAR_CREDIT_APPROVED = "creditApproved";
    public static final String VAR_CREDIT_SCORE = "creditScore";
    public static final String VAR_CREDIT_REASON = "creditReason";
    public static final String VAR_REQUIRES_MANUAL_APPROVAL = "requiresManualApproval";
    public static final String VAR_APPROVED = "approved";
    public static final String VAR_APPROVER = "approver";
    public static final String VAR_RESERVATION_ID = "reservationId";
    public static final String VAR_INVENTORY_STATUS = "inventoryStatus";
    public static final String VAR_PAYMENT_ID = "paymentId";
    public static final String VAR_PAYMENT_STATUS = "paymentStatus";
    public static final String VAR_REFUND_ID = "refundId";
    public static final String VAR_SHIPMENT_ID = "shipmentId";
    public static final String VAR_FULFILMENT_STATUS = "fulfilmentStatus";

    public static final String STATUS_RESERVED = "reserved";
    public static final String STATUS_RELEASED = "released";
    public static final String STATUS_COLLECTED = "collected";
    public static final String STATUS_REFUNDED = "refunded";
    public static final String STATUS_SHIPPED = "shipped";

    private OrderProcess() {}
}
