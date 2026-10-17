package com.arpanbom.orderworkflow.fake;

import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a credit bureau. Customer ids with the prefix
 * {@code blocked-} are declined. Every other id is approved. Scores are fixed
 * demo values, not a bureau scale.
 */
@Component
public class FakeCreditBureau implements CreditBureau {

    public static final String BLOCKED_PREFIX = "blocked-";
    public static final int APPROVED_SCORE = 760;
    public static final int DECLINED_SCORE = 410;

    @Override
    public CreditDecision assess(String customerId) {
        if (customerId != null && customerId.startsWith(BLOCKED_PREFIX)) {
            return new CreditDecision(false, DECLINED_SCORE, "Demo bureau declined the customer.");
        }
        return new CreditDecision(true, APPROVED_SCORE, "Demo bureau approved the customer.");
    }
}
