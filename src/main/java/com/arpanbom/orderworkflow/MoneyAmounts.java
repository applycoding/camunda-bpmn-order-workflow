package com.arpanbom.orderworkflow;

import java.math.BigDecimal;

public final class MoneyAmounts {

    private MoneyAmounts() {}

    public static BigDecimal toDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text.trim());
        }
        throw new IllegalArgumentException("Order amount is missing or not numeric.");
    }
}
