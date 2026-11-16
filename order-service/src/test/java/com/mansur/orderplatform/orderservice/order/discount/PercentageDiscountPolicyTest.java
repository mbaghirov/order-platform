package com.mansur.orderplatform.orderservice.order.discount;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PercentageDiscountPolicyTest {

    private final DiscountPolicy policy =
            new PercentageDiscountPolicy(new BigDecimal("100"), new BigDecimal("10"));

    @Test
    void noDiscountBelowThreshold() {
        assertThat(policy.discountFor(new BigDecimal("99.99"))).isEqualByComparingTo("0");
    }

    @Test
    void discountAtThreshold() {
        assertThat(policy.discountFor(new BigDecimal("100.00"))).isEqualByComparingTo("10.00");
    }

    @Test
    void discountIsRoundedToCents() {
        assertThat(policy.discountFor(new BigDecimal("123.45"))).isEqualByComparingTo("12.35");
    }

    @Test
    void rejectsInvalidPercent() {
        assertThatThrownBy(() -> new PercentageDiscountPolicy(BigDecimal.ZERO, new BigDecimal("150")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
