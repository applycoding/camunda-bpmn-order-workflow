package com.arpanbom.orderworkflow.worker;

import com.arpanbom.orderworkflow.OrderProcess;
import com.arpanbom.orderworkflow.fake.CreditBureau;
import com.arpanbom.orderworkflow.fake.CreditDecision;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CreditCheckWorker {

    private final CreditBureau creditBureau;

    public CreditCheckWorker(CreditBureau creditBureau) {
        this.creditBureau = creditBureau;
    }

    @JobWorker(type = OrderProcess.CHECK_CREDIT)
    public Map<String, Object> checkCredit(
            @Variable(name = OrderProcess.VAR_CUSTOMER_ID) String customerId) {
        CreditDecision decision = creditBureau.assess(customerId);
        return Map.of(
                OrderProcess.VAR_CREDIT_APPROVED, decision.approved(),
                OrderProcess.VAR_CREDIT_SCORE, decision.score(),
                OrderProcess.VAR_CREDIT_REASON, decision.reason());
    }
}
