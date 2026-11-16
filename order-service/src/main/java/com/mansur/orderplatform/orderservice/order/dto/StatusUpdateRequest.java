package com.mansur.orderplatform.orderservice.order.dto;

import com.mansur.orderplatform.orderservice.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
