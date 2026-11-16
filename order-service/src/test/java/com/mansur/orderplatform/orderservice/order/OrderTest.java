package com.mansur.orderplatform.orderservice.order;

import com.mansur.orderplatform.orderservice.common.exception.BusinessException;
import com.mansur.orderplatform.orderservice.common.exception.InsufficientStockException;
import com.mansur.orderplatform.orderservice.common.exception.InvalidStatusTransitionException;
import com.mansur.orderplatform.orderservice.order.discount.NoDiscountPolicy;
import com.mansur.orderplatform.orderservice.order.discount.PercentageDiscountPolicy;
import com.mansur.orderplatform.orderservice.product.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure domain tests: no Spring, no database.
 */
class OrderTest {

    private final Product keyboard = new Product("Keyboard", null, new BigDecimal("45.00"), 10);
    private final Product mouse = new Product("Mouse", null, new BigDecimal("20.00"), 5);

    @Test
    void addItem_reservesStockAndCopiesPrice() {
        Order order = new Order("customer@example.com");

        order.addItem(keyboard, 2);

        assertThat(keyboard.getStockQuantity()).isEqualTo(8);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("45.00");
        assertThat(order.getItems().get(0).lineTotal()).isEqualByComparingTo("90.00");
    }

    @Test
    void addItem_failsWhenStockIsTooLow() {
        Order order = new Order("customer@example.com");

        assertThatThrownBy(() -> order.addItem(mouse, 6))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("available 5");
        assertThat(mouse.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void applyPricing_withoutDiscount() {
        Order order = new Order("customer@example.com");
        order.addItem(keyboard, 1);
        order.addItem(mouse, 1);

        order.applyPricing(new NoDiscountPolicy());

        assertThat(order.getSubtotal()).isEqualByComparingTo("65.00");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("65.00");
    }

    @Test
    void applyPricing_withPercentageDiscountAboveThreshold() {
        Order order = new Order("customer@example.com");
        order.addItem(keyboard, 2);   // 90.00
        order.addItem(mouse, 1);      // 20.00 -> subtotal 110.00

        order.applyPricing(new PercentageDiscountPolicy(new BigDecimal("100"), new BigDecimal("10")));

        assertThat(order.getSubtotal()).isEqualByComparingTo("110.00");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("11.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("99.00");
    }

    @Test
    void changeStatus_followsLifecycle() {
        Order order = new Order("customer@example.com");

        order.changeStatus(OrderStatus.PAID);
        order.changeStatus(OrderStatus.SHIPPED);
        order.changeStatus(OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void changeStatus_rejectsSkippingSteps() {
        Order order = new Order("customer@example.com");

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.SHIPPED))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CREATED to SHIPPED");
    }

    @Test
    void cancel_returnsReservedStock() {
        Order order = new Order("customer@example.com");
        order.addItem(keyboard, 3);
        assertThat(keyboard.getStockQuantity()).isEqualTo(7);

        order.changeStatus(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(keyboard.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void deliveredOrder_cannotBeCancelled() {
        Order order = new Order("customer@example.com");
        order.changeStatus(OrderStatus.PAID);
        order.changeStatus(OrderStatus.SHIPPED);
        order.changeStatus(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CANCELLED))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void itemsCannotBeAddedAfterPayment() {
        Order order = new Order("customer@example.com");
        order.changeStatus(OrderStatus.PAID);

        assertThatThrownBy(() -> order.addItem(keyboard, 1))
                .isInstanceOf(BusinessException.class);
    }
}
