package com.mansur.orderplatform.orderservice.order.discount;

import java.math.BigDecimal;

public class NoDiscountPolicy implements DiscountPolicy {

    @Override
    public BigDecimal discountFor(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }
}
