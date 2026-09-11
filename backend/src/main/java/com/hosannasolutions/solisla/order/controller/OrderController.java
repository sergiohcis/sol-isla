package com.hosannasolutions.solisla.order.controller;

import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.dto.OrderItemResponse;
import com.hosannasolutions.solisla.order.dto.OrderResponse;
import com.hosannasolutions.solisla.order.providedService.OrderService;
import com.hosannasolutions.solisla.order.repository.OrderItemRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public order tracking by order number — no additional secret required (design doc §53); the
 *  order number itself, drawn from a sequence rather than being trivially guessable, is the only
 *  gate for this MVP. Deliberately at {@code /api/orders/track/**} rather than
 *  {@code /api/orders/{orderNumber}} to keep it unambiguously separate from
 *  {@code /api/admin/orders/**} (Phase 6) in the security matcher list. */
@RestController
@RequestMapping("/api/orders/track")
public class OrderController {

    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;

    public OrderController(OrderService orderService, OrderItemRepository orderItemRepository) {
        this.orderService = orderService;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse get(@PathVariable String orderNumber) {
        Order order = orderService.getByOrderNumber(orderNumber);
        var items = orderItemRepository.findByOrderIdOrderById(order.getId()).stream().map(OrderItemResponse::from).toList();
        return OrderResponse.from(order, items);
    }
}
