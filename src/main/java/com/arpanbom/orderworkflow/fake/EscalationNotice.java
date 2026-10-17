package com.arpanbom.orderworkflow.fake;

import java.math.BigDecimal;

public record EscalationNotice(String orderId, BigDecimal amount, String currency) {}
