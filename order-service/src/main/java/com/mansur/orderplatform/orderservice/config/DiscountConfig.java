package com.mansur.orderplatform.orderservice.config;

import com.mansur.orderplatform.orderservice.order.discount.DiscountPolicy;
import com.mansur.orderplatform.orderservice.order.discount.NoDiscountPolicy;
import com.mansur.orderplatform.orderservice.order.discount.PercentageDiscountPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Chooses the discount rule from configuration. OrderService only knows the
 * DiscountPolicy interface, not which implementation is active.
 */
@Configuration
public class DiscountConfig {

    @Bean
    public DiscountPolicy discountPolicy(@Value("${app.discount.enabled}") boolean enabled,
                                         @Value("${app.discount.threshold}") BigDecimal threshold,
                                         @Value("${app.discount.percent}") BigDecimal percent) {
        return enabled ? new PercentageDiscountPolicy(threshold, percent) : new NoDiscountPolicy();
    }
}
