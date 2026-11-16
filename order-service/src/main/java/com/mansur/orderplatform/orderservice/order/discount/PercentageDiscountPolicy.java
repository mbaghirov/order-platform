package com.mansur.orderplatform.orderservice.order.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Gives a percentage discount when the subtotal reaches a threshold,
 * for example 10% off orders of 100 or more.
 */
public class PercentageDiscountPolicy implements DiscountPolicy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal threshold;
    private final BigDecimal percent;

    public PercentageDiscountPolicy(BigDecimal threshold, BigDecimal percent) {
        if (threshold == null || threshold.signum() < 0) {
            throw new IllegalArgumentException("Threshold must be zero or positive");
        }
        if (percent == null || percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Percent must be between 0 and 100");
        }
        this.threshold = threshold;
        this.percent = percent;
    }

    @Override
    public BigDecimal discountFor(BigDecimal subtotal) {
        if (subtotal.compareTo(threshold) < 0) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
