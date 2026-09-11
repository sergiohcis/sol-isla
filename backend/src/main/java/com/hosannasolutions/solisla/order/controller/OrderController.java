package com.hosannasolutions.solisla.order.controller;

import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.dto.OrderItemResponse;
import com.hosannasolutions.solisla.order.dto.OrderResponse;
import com.hosannasolutions.solisla.order.dto.OrderTrackingRequest;
import com.hosannasolutions.solisla.order.providedService.OrderService;
import com.hosannasolutions.solisla.order.repository.OrderItemRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public order tracking — POST, not {@code GET /{orderNumber}}, because the order number alone
 * is not a valid credential (design doc §53: "do not expose another customer's order merely
 * because someone knows a sequential order number" — and ours are sequential). The caller must
 * also supply the phone number the order was placed with; a mismatch and a nonexistent order
 * number produce the identical 404, so this endpoint can't be used to enumerate valid order
 * numbers or confirm/deny a phone number's association with one.
 * <p>
 * Deliberately at {@code /api/orders/track/**} rather than {@code /api/orders/{orderNumber}} to
 * keep it unambiguously separate from {@code /api/admin/orders/**} (Phase 6) in the security
 * matcher list.
 */
@RestController
@RequestMapping("/api/orders/track")
public class OrderController {

    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;

    public OrderController(OrderService orderService, OrderItemRepository orderItemRepository) {
        this.orderService = orderService;
        this.orderItemRepository = orderItemRepository;
    }

    @PostMapping
    public OrderResponse track(@Valid @RequestBody OrderTrackingRequest request) {
        Order order = orderService.getByOrderNumberAndPhone(request.orderNumber(), request.phone());
        var items = orderItemRepository.findByOrderIdOrderById(order.getId()).stream().map(OrderItemResponse::from).toList();
        return OrderResponse.from(order, items);
    }
}
