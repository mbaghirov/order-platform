package com.mansur.orderplatform.orderservice.common.exception;

import com.mansur.orderplatform.orderservice.order.OrderStatus;

public class InvalidStatusTransitionException extends BusinessException {

    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Order status cannot change from " + from + " to " + to);
    }
}
