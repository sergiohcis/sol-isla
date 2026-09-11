package com.hosannasolutions.solisla.order.controller;

import com.hosannasolutions.solisla.common.api.PageResponse;
import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.OrderStatus;
import com.hosannasolutions.solisla.order.dto.OrderCancelRequest;
import com.hosannasolutions.solisla.order.dto.OrderItemResponse;
import com.hosannasolutions.solisla.order.dto.OrderResponse;
import com.hosannasolutions.solisla.order.dto.OrderStatusChangeRequest;
import com.hosannasolutions.solisla.order.dto.OrderSummaryResponse;
import com.hosannasolutions.solisla.order.providedService.OrderService;
import com.hosannasolutions.solisla.order.repository.OrderItemRepository;
import com.hosannasolutions.solisla.security.userdetails.SolIslaUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Phase 6 admin order workflow. Kept separate from the public {@code OrderController}
 *  (which stays at {@code /api/orders/track/**}) — see that controller's own doc comment about
 *  the two staying unambiguously distinct in the security matcher list. */
@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;

    public OrderAdminController(OrderService orderService, OrderItemRepository orderItemRepository) {
        this.orderService = orderService;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public PageResponse<OrderSummaryResponse> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.searchAdmin(status, q, pageable);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public OrderResponse get(@PathVariable UUID orderId) {
        return toResponse(orderService.getById(orderId));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('ORDER_UPDATE_STATUS')")
    public OrderResponse changeStatus(@PathVariable UUID orderId, @Valid @RequestBody OrderStatusChangeRequest request,
                                       @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return toResponse(orderService.changeStatus(orderId, request.status(), principal.getUserId()));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('ORDER_CANCEL')")
    public OrderResponse cancel(@PathVariable UUID orderId, @Valid @RequestBody OrderCancelRequest request,
                                 @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return toResponse(orderService.cancel(orderId, request.reason(), principal.getUserId()));
    }

    private OrderResponse toResponse(Order order) {
        var items = orderItemRepository.findByOrderIdOrderById(order.getId()).stream().map(OrderItemResponse::from).toList();
        return OrderResponse.from(order, items);
    }
}
