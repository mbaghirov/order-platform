package com.mansur.orderplatform.orderservice.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email must be valid")
        @Size(max = 150, message = "Customer email must be at most 150 characters")
        String customerEmail,

        @NotEmpty(message = "Order must contain at least one item")
        @Size(max = 50, message = "Order can contain at most 50 items")
        List<@NotNull(message = "Item must not be null") @Valid OrderItemRequest> items
) {
}
