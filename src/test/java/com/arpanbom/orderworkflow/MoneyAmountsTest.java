package com.arpanbom.orderworkflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyAmountsTest {

    @Test
    void acceptsDecimalNumberAndTextAmounts() {
        assertEquals(new BigDecimal("12.50"), MoneyAmounts.toDecimal(new BigDecimal("12.50")));
        assertEquals(0, MoneyAmounts.toDecimal(12).compareTo(new BigDecimal("12.0")));
        assertEquals(0, MoneyAmounts.toDecimal(12.5d).compareTo(new BigDecimal("12.5")));
        assertEquals(new BigDecimal("99.00"), MoneyAmounts.toDecimal("99.00"));
    }

    @Test
    void rejectsMissingAmounts() {
        assertThrows(IllegalArgumentException.class, () -> MoneyAmounts.toDecimal(null));
        assertThrows(IllegalArgumentException.class, () -> MoneyAmounts.toDecimal("  "));
        assertThrows(IllegalArgumentException.class, () -> MoneyAmounts.toDecimal("n/a"));
    }
}
