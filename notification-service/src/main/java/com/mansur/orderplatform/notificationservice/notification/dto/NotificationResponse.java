package com.mansur.orderplatform.notificationservice.notification.dto;

import com.mansur.orderplatform.notificationservice.notification.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long orderId,
        String eventType,
        String recipient,
        String subject,
        String message,
        Instant createdAt
) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getOrderId(), n.getEventType(),
                n.getRecipient(), n.getSubject(), n.getMessage(), n.getCreatedAt());
    }
}
