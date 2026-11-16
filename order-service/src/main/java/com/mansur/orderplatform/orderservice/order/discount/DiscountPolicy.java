package com.mansur.orderplatform.orderservice.order.discount;

import java.math.BigDecimal;

/**
 * Strategy for calculating an order discount.
 */
public interface DiscountPolicy {

    BigDecimal discountFor(BigDecimal subtotal);
}
