package com.mansur.orderplatform.notificationservice.notification;

import com.mansur.orderplatform.common.event.OrderEvent;
import org.springframework.stereotype.Component;

/**
 * Turns an order event into the text a customer would receive.
 */
@Component
public class NotificationMessageFactory {

    public Notification create(OrderEvent event) {
        String subject;
        String message;

        switch (event.type()) {
            case ORDER_CREATED -> {
                subject = "Order #" + event.orderId() + " received";
                message = "Thank you for your order! Total: " + event.totalAmount()
                        + ". We will let you know when it ships.";
            }
            case ORDER_STATUS_CHANGED -> {
                subject = "Order #" + event.orderId() + " is now " + event.status();
                message = "Your order status changed to " + event.status() + ".";
            }
            case ORDER_CANCELLED -> {
                subject = "Order #" + event.orderId() + " cancelled";
                message = "Your order was cancelled. If you already paid, the amount "
                        + event.totalAmount() + " will be refunded.";
            }
            default -> throw new IllegalArgumentException("Unsupported event type: " + event.type());
        }

        return new Notification(event.eventId(), event.orderId(), event.type().name(),
                event.customerEmail(), subject, message);
    }
}
