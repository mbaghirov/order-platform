package com.mansur.orderplatform.orderservice.order;

import com.mansur.orderplatform.orderservice.common.BaseEntity;
import com.mansur.orderplatform.orderservice.common.exception.BusinessException;
import com.mansur.orderplatform.orderservice.common.exception.InvalidStatusTransitionException;
import com.mansur.orderplatform.orderservice.order.discount.DiscountPolicy;
import com.mansur.orderplatform.orderservice.product.Product;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Order aggregate. All changes go through its methods (addItem, applyPricing,
 * changeStatus), which keep stock, totals and status consistent.
 */
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    protected Order() {
        // required by JPA
    }

    public Order(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    /**
     * Reserves stock and adds a line. Only allowed while the order is new.
     */
    public void addItem(Product product, int quantity) {
        if (status != OrderStatus.CREATED) {
            throw new BusinessException("Items can only be added to a new order");
        }
        product.decreaseStock(quantity);
        items.add(new OrderItem(this, product, quantity));
    }

    /**
     * Calculates subtotal, discount and total. The discount rule is passed in,
     * so new rules can be added without changing this class (polymorphism).
     */
    public void applyPricing(DiscountPolicy discountPolicy) {
        this.subtotal = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = discountPolicy.discountFor(subtotal).setScale(2, RoundingMode.HALF_UP);
        this.discountAmount = discount.min(subtotal);
        this.totalAmount = subtotal.subtract(discountAmount);
    }

    /**
     * Moves the order to a new status if the lifecycle allows it.
     * Cancelling returns the reserved stock.
     */
    public void changeStatus(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        if (target == OrderStatus.CANCELLED) {
            items.forEach(item -> item.getProduct().increaseStock(item.getQuantity()));
        }
        this.status = target;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
