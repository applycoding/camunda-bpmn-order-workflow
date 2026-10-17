package com.arpanbom.orderworkflow.worker;

import com.arpanbom.orderworkflow.MoneyAmounts;
import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.fake.PaymentDeclinedException;
import com.arpanbom.orderworkflow.fake.PaymentGateway;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.exception.CamundaError;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentWorker {

    private final PaymentGateway payments;

    public PaymentWorker(PaymentGateway payments) {
        this.payments = payments;
    }

    @JobWorker(type = OrderProcess.COLLECT_PAYMENT)
    public Map<String, Object> collectPayment(
            @Variable(name = OrderProcess.VAR_ORDER_ID) String orderId,
            @Variable(name = OrderProcess.VAR_AMOUNT) Object amount,
            @Variable(name = OrderProcess.VAR_CURRENCY) String currency) {
        try {
            String paymentId = payments.charge(orderId, MoneyAmounts.toDecimal(amount), currency);
            return Map.of(
                    OrderProcess.VAR_PAYMENT_ID, paymentId,
                    OrderProcess.VAR_PAYMENT_STATUS, OrderProcess.STATUS_COLLECTED);
        } catch (PaymentDeclinedException declined) {
            throw CamundaError.bpmnError(OrderProcess.PAYMENT_FAILED, declined.getMessage());
        }
    }

    @JobWorker(type = OrderProcess.REFUND_PAYMENT)
    public Map<String, Object> refundPayment(
            @Variable(name = OrderProcess.VAR_ORDER_ID) String orderId,
            @Variable(name = OrderProcess.VAR_PAYMENT_ID) String paymentId) {
        String refundId = payments.refund(orderId, paymentId);
        return Map.of(
                OrderProcess.VAR_REFUND_ID, refundId,
                OrderProcess.VAR_PAYMENT_STATUS, OrderProcess.STATUS_REFUNDED);
    }
}
