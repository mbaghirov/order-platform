package com.mansur.orderplatform.notificationservice.messaging;

import com.mansur.orderplatform.common.event.OrderEvent;
import com.mansur.orderplatform.notificationservice.notification.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order events. If handling fails, the error handler in KafkaConfig
 * retries 3 times and then moves the message to the dead-letter topic.
 */
@Component
public class OrderEventListener {

    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.topic.order-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderEvent(OrderEvent event) {
        notificationService.handle(event);
    }
}
