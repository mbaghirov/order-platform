package com.mansur.orderplatform.notificationservice.notification;

import com.mansur.orderplatform.common.event.OrderEvent;
import com.mansur.orderplatform.common.event.OrderEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private final NotificationMessageFactory messageFactory = new NotificationMessageFactory();

    @Test
    void createsNotificationForNewEvent() {
        var service = new NotificationService(notificationRepository, messageFactory);
        OrderEvent event = OrderEvent.of(OrderEventType.ORDER_CREATED, 42L, "c@example.com",
                "CREATED", new BigDecimal("99.00"));
        when(notificationRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean created = service.handle(event);

        assertThat(created).isTrue();
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getRecipient()).isEqualTo("c@example.com");
        assertThat(saved.getValue().getSubject()).isEqualTo("Order #42 received");
        assertThat(saved.getValue().getMessage()).contains("99.00");
        assertThat(saved.getValue().getEventId()).isEqualTo(event.eventId());
    }

    @Test
    void ignoresDuplicateEvent() {
        var service = new NotificationService(notificationRepository, messageFactory);
        OrderEvent event = OrderEvent.of(OrderEventType.ORDER_CREATED, 42L, "c@example.com",
                "CREATED", new BigDecimal("99.00"));
        when(notificationRepository.existsByEventId(event.eventId())).thenReturn(true);

        boolean created = service.handle(event);

        assertThat(created).isFalse();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void messageDependsOnEventType() {
        Notification shipped = messageFactory.create(OrderEvent.of(OrderEventType.ORDER_STATUS_CHANGED, 7L,
                "c@example.com", "SHIPPED", new BigDecimal("10.00")));
        Notification cancelled = messageFactory.create(OrderEvent.of(OrderEventType.ORDER_CANCELLED, 7L,
                "c@example.com", "CANCELLED", new BigDecimal("10.00")));

        assertThat(shipped.getSubject()).isEqualTo("Order #7 is now SHIPPED");
        assertThat(cancelled.getSubject()).isEqualTo("Order #7 cancelled");
        assertThat(cancelled.getMessage()).contains("refunded");
    }
}
