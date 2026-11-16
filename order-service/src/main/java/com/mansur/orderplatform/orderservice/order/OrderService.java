package com.mansur.orderplatform.orderservice.order;

import com.mansur.orderplatform.common.event.OrderEvent;
import com.mansur.orderplatform.common.event.OrderEventType;
import com.mansur.orderplatform.orderservice.common.PageResponse;
import com.mansur.orderplatform.orderservice.common.exception.ResourceNotFoundException;
import com.mansur.orderplatform.orderservice.order.discount.DiscountPolicy;
import com.mansur.orderplatform.orderservice.order.dto.CreateOrderRequest;
import com.mansur.orderplatform.orderservice.order.dto.OrderItemRequest;
import com.mansur.orderplatform.orderservice.order.dto.OrderResponse;
import com.mansur.orderplatform.orderservice.product.Product;
import com.mansur.orderplatform.orderservice.product.ProductCacheEvictor;
import com.mansur.orderplatform.orderservice.product.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Places orders and moves them through their lifecycle.
 *
 * Every change publishes an OrderEvent. The event is sent to Kafka only after the
 * database transaction commits (see OrderEventPublisher), so consumers never hear
 * about an order that was rolled back.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DiscountPolicy discountPolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductCacheEvictor productCacheEvictor;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        DiscountPolicy discountPolicy,
                        ApplicationEventPublisher eventPublisher,
                        ProductCacheEvictor productCacheEvictor) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.discountPolicy = discountPolicy;
        this.eventPublisher = eventPublisher;
        this.productCacheEvictor = productCacheEvictor;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        // The same product may appear twice in the request: merge the quantities
        Map<Long, Integer> quantityByProduct = request.items().stream()
                .collect(Collectors.groupingBy(OrderItemRequest::productId, LinkedHashMap::new,
                        Collectors.summingInt(OrderItemRequest::quantity)));

        Map<Long, Product> products = productRepository.findAllById(quantityByProduct.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Order order = new Order(normalize(request.customerEmail()));
        quantityByProduct.forEach((productId, quantity) -> {
            Product product = products.get(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + productId);
            }
            order.addItem(product, quantity);
        });
        order.applyPricing(discountPolicy);

        Order saved = orderRepository.saveAndFlush(order);

        productCacheEvictor.evictAfterCommit(quantityByProduct.keySet());
        eventPublisher.publishEvent(toEvent(OrderEventType.ORDER_CREATED, saved));
        return OrderResponse.from(saved);
    }

    public OrderResponse getById(Long id) {
        return OrderResponse.from(findOrder(id));
    }

    public PageResponse<OrderResponse> list(String customerEmail, Pageable pageable) {
        var page = (customerEmail == null || customerEmail.isBlank())
                ? orderRepository.findAll(pageable)
                : orderRepository.findByCustomerEmail(normalize(customerEmail), pageable);
        return PageResponse.of(page, OrderResponse::from);
    }

    @Transactional
    public OrderResponse changeStatus(Long id, OrderStatus target) {
        Order order = findOrder(id);
        order.changeStatus(target);
        Order saved = orderRepository.saveAndFlush(order);

        OrderEventType type = OrderEventType.ORDER_STATUS_CHANGED;
        if (target == OrderStatus.CANCELLED) {
            type = OrderEventType.ORDER_CANCELLED;
            List<Long> productIds = saved.getItems().stream().map(item -> item.getProduct().getId()).toList();
            productCacheEvictor.evictAfterCommit(productIds);
        }
        eventPublisher.publishEvent(toEvent(type, saved));
        return OrderResponse.from(saved);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private static OrderEvent toEvent(OrderEventType type, Order order) {
        return OrderEvent.of(type, order.getId(), order.getCustomerEmail(),
                order.getStatus().name(), order.getTotalAmount());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
