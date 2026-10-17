package com.arpanbom.orderworkflow.worker;

import com.arpanbom.orderworkflow.MoneyAmounts;
import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.fake.EscalationNotice;
import com.arpanbom.orderworkflow.fake.EscalationNotifier;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EscalationWorker {

    private final EscalationNotifier notifier;

    public EscalationWorker(EscalationNotifier notifier) {
        this.notifier = notifier;
    }

    @JobWorker(type = OrderProcess.ESCALATE_APPROVAL)
    public Map<String, Object> escalate(
            @Variable(name = OrderProcess.VAR_ORDER_ID) String orderId,
            @Variable(name = OrderProcess.VAR_AMOUNT) Object amount,
            @Variable(name = OrderProcess.VAR_CURRENCY) String currency) {
        notifier.notifyApprovers(new EscalationNotice(orderId, MoneyAmounts.toDecimal(amount), currency));
        return Map.of("escalated", true);
    }
}
