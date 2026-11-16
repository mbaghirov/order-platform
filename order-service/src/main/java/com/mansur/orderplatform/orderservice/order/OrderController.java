package com.mansur.orderplatform.orderservice.order;

import com.mansur.orderplatform.orderservice.common.PageResponse;
import com.mansur.orderplatform.orderservice.order.dto.CreateOrderRequest;
import com.mansur.orderplatform.orderservice.order.dto.OrderResponse;
import com.mansur.orderplatform.orderservice.order.dto.StatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Place orders and move them through their lifecycle (events go to Kafka)")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place an order: reserves stock, applies discount, publishes ORDER_CREATED")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List orders, optionally for one customer")
    public PageResponse<OrderResponse> list(@RequestParam(required = false) String customerEmail,
                                            @ParameterObject @PageableDefault(size = 20, sort = "createdAt",
                                                    direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.list(customerEmail, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change order status (PAID, SHIPPED, DELIVERED, CANCELLED); cancelling returns stock")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return orderService.changeStatus(id, request.status());
    }
}
