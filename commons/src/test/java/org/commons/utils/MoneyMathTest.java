package org.commons.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyMathTest {

    @Test
    void fifteenPercentOnRoundAmountIsExact() {
        assertEquals(new BigDecimal("1150.00"),
                MoneyMath.totalRepayment(new BigDecimal("1000"), new BigDecimal("15")));
    }

    @Test
    void largeAmountsHaveNoFloatDrift() {
        // With float 0.15f this used to give 1,150,000.01 after rounding.
        assertEquals(new BigDecimal("1150000.00"),
                MoneyMath.totalRepayment(new BigDecimal("1000000.00"), new BigDecimal("15")));
    }

    @Test
    void interestRoundsHalfEvenToTheNgwee() {
        assertEquals(new BigDecimal("0.12"),
                MoneyMath.interest(new BigDecimal("0.83"), new BigDecimal("15")));
        assertEquals(new BigDecimal("38.53"),
                MoneyMath.interest(new BigDecimal("256.85"), new BigDecimal("15")));
    }

    @Test
    void missingRateMeansNoInterest() {
        assertEquals(new BigDecimal("500.00"),
                MoneyMath.totalRepayment(new BigDecimal("500"), null));
    }

    @Test
    void negativeValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MoneyMath.interest(new BigDecimal("-1"), new BigDecimal("15")));
        assertThrows(IllegalArgumentException.class,
                () -> MoneyMath.interest(new BigDecimal("1"), new BigDecimal("-15")));
    }
}
