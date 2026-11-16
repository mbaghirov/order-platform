package com.mansur.orderplatform.notificationservice.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventId(UUID eventId);

    Page<Notification> findByRecipient(String recipient, Pageable pageable);

    List<Notification> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
