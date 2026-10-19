package org.commons.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Exact Kwacha arithmetic. All money is BigDecimal at 2 decimal places (ngwee);
 * rates are BigDecimal percentages. Never use float or double for money.
 */
public final class MoneyMath {

    public static final int MONEY_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private MoneyMath() {
    }

    /** Interest on a principal, rounded to the ngwee. */
    public static BigDecimal interest(BigDecimal principal, BigDecimal ratePercent) {
        if (principal == null) {
            throw new IllegalArgumentException("principal is required");
        }
        BigDecimal rate = ratePercent == null ? BigDecimal.ZERO : ratePercent;
        if (principal.signum() < 0 || rate.signum() < 0) {
            throw new IllegalArgumentException("principal and rate must not be negative");
        }
        return principal.multiply(rate).divide(HUNDRED, MONEY_SCALE, ROUNDING);
    }

    /** Principal plus interest, rounded to the ngwee. */
    public static BigDecimal totalRepayment(BigDecimal principal, BigDecimal ratePercent) {
        return principal.setScale(MONEY_SCALE, ROUNDING).add(interest(principal, ratePercent));
    }
}
