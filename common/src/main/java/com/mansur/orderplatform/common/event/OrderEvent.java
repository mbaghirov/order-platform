package com.mansur.orderplatform.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Message published by order-service to the "order-events" Kafka topic.
 *
 * @param eventId unique id, used by consumers to ignore duplicate deliveries
 */
public record OrderEvent(
        UUID eventId,
        OrderEventType type,
        Long orderId,
        String customerEmail,
        String status,
        BigDecimal totalAmount,
        Instant occurredAt
) {

    public static OrderEvent of(OrderEventType type, Long orderId, String customerEmail,
                                String status, BigDecimal totalAmount) {
        return new OrderEvent(UUID.randomUUID(), type, orderId, customerEmail, status, totalAmount, Instant.now());
    }
}
