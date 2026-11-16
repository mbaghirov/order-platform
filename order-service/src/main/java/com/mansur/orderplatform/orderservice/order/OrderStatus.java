package com.mansur.orderplatform.orderservice.order;

/**
 * Order lifecycle. Each status decides which statuses may follow it:
 *
 * CREATED -> PAID -> SHIPPED -> DELIVERED
 *    \         \
 *     +---------+--> CANCELLED
 */
public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case CREATED -> next == PAID || next == CANCELLED;
            case PAID -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
