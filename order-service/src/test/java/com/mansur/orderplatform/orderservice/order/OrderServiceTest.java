package com.mansur.orderplatform.orderservice.order;

import com.mansur.orderplatform.common.event.OrderEvent;
import com.mansur.orderplatform.common.event.OrderEventType;
import com.mansur.orderplatform.orderservice.common.exception.ResourceNotFoundException;
import com.mansur.orderplatform.orderservice.order.discount.NoDiscountPolicy;
import com.mansur.orderplatform.orderservice.order.dto.CreateOrderRequest;
import com.mansur.orderplatform.orderservice.order.dto.OrderItemRequest;
import com.mansur.orderplatform.orderservice.order.dto.OrderResponse;
import com.mansur.orderplatform.orderservice.product.Product;
import com.mansur.orderplatform.orderservice.product.ProductCacheEvictor;
import com.mansur.orderplatform.orderservice.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ProductCacheEvictor productCacheEvictor;

    private OrderService orderService;
    private Product keyboard;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository, new NoDiscountPolicy(),
                eventPublisher, productCacheEvictor);
        keyboard = new Product("Keyboard", null, new BigDecimal("45.00"), 10);
        ReflectionTestUtils.setField(keyboard, "id", 1L);
    }

    @Test
    void create_mergesDuplicateProductsAndPublishesEvent() {
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(keyboard));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 100L);
            return order;
        });

        OrderResponse response = orderService.create(new CreateOrderRequest(" Customer@Example.com ", List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(1L, 1))));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.customerEmail()).isEqualTo("customer@example.com");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
        assertThat(response.totalAmount()).isEqualByComparingTo("135.00");
        assertThat(keyboard.getStockQuantity()).isEqualTo(7);

        ArgumentCaptor<OrderEvent> event = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(OrderEventType.ORDER_CREATED);
        assertThat(event.getValue().orderId()).isEqualTo(100L);
        verify(productCacheEvictor).evictAfterCommit(anyCollection());
    }

    @Test
    void create_failsForUnknownProduct() {
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest("c@example.com",
                List.of(new OrderItemRequest(99L, 1)))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(orderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void cancel_publishesCancelledEvent() {
        Order order = new Order("c@example.com");
        order.addItem(keyboard, 2);
        ReflectionTestUtils.setField(order, "id", 5L);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        OrderResponse response = orderService.changeStatus(5L, OrderStatus.CANCELLED);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(keyboard.getStockQuantity()).isEqualTo(10);

        ArgumentCaptor<OrderEvent> event = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(OrderEventType.ORDER_CANCELLED);
    }

    @Test
    void pay_publishesStatusChangedEvent() {
        Order order = new Order("c@example.com");
        ReflectionTestUtils.setField(order, "id", 6L);
        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        orderService.changeStatus(6L, OrderStatus.PAID);

        ArgumentCaptor<OrderEvent> event = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(OrderEventType.ORDER_STATUS_CHANGED);
        assertThat(event.getValue().status()).isEqualTo("PAID");
        verifyNoInteractions(productCacheEvictor);
    }
}
