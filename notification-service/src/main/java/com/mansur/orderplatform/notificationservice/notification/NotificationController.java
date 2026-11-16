package com.mansur.orderplatform.notificationservice.notification;

import com.mansur.orderplatform.notificationservice.common.PageResponse;
import com.mansur.orderplatform.notificationservice.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notifications created from Kafka order events")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Notifications sent to one customer")
    public PageResponse<NotificationResponse> byRecipient(
            @RequestParam String recipient,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(notificationService.findByRecipient(recipient, pageable));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "All notifications for one order, oldest first")
    public List<NotificationResponse> byOrder(@PathVariable Long orderId) {
        return notificationService.findByOrder(orderId);
    }
}
