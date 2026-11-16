package com.mansur.orderplatform.notificationservice.notification;

import com.mansur.orderplatform.common.event.OrderEvent;
import com.mansur.orderplatform.notificationservice.notification.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMessageFactory messageFactory;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationMessageFactory messageFactory) {
        this.notificationRepository = notificationRepository;
        this.messageFactory = messageFactory;
    }

    /**
     * Kafka delivers "at least once", so the same event can arrive twice.
     * The event id check makes handling idempotent.
     *
     * @return true if a notification was created, false if the event was a duplicate
     */
    @Transactional
    public boolean handle(OrderEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Skipping duplicate event {} for order {}", event.eventId(), event.orderId());
            return false;
        }

        Notification notification = notificationRepository.save(messageFactory.create(event));

        // A real system would call an email/SMS provider here
        log.info("Notification sent to {}: {}", notification.getRecipient(), notification.getSubject());
        return true;
    }

    public Page<NotificationResponse> findByRecipient(String recipient, Pageable pageable) {
        return notificationRepository
                .findByRecipient(recipient.trim().toLowerCase(Locale.ROOT), pageable)
                .map(NotificationResponse::from);
    }

    public List<NotificationResponse> findByOrder(Long orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
