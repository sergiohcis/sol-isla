package com.hosannasolutions.solisla.order.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.common.api.PageResponse;
import com.hosannasolutions.solisla.common.util.Phones;
import com.hosannasolutions.solisla.inventory.providedService.InventoryService;
import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.OrderItem;
import com.hosannasolutions.solisla.order.OrderStatus;
import com.hosannasolutions.solisla.order.dto.OrderSummaryResponse;
import com.hosannasolutions.solisla.order.exception.InvalidOrderStatusTransitionException;
import com.hosannasolutions.solisla.order.exception.OrderNotFoundException;
import com.hosannasolutions.solisla.order.repository.OrderItemRepository;
import com.hosannasolutions.solisla.order.repository.OrderRepository;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    /** Design doc §17/CLAUDE.md rule 5, mirroring {@code ProductServiceImpl.ALLOWED_TRANSITIONS}.
     *  {@code REJECTED}, {@code CANCELLED}, and {@code RETURNED} are terminal and restock inventory
     *  on entry (see {@link #restocksOnEntry}); {@code DELIVERED} and {@code FAILED_DELIVERY} do
     *  not restock by themselves (goods are still out — a delivery can be retried, or later marked
     *  {@code RETURNED} once they actually come back). */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = allowedTransitions();

    /** Reachable only through {@link #cancel}, which requires {@code ORDER_CANCEL} rather than
     *  {@code ORDER_UPDATE_STATUS} (see {@code RolePermissions}) — cancelling is allowed any time
     *  before the order has actually been delivered. */
    private static final Set<OrderStatus> CANCELLABLE_FROM = EnumSet.of(
            OrderStatus.PENDING_CONFIRMATION, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
            OrderStatus.READY_FOR_DELIVERY, OrderStatus.OUT_FOR_DELIVERY);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             InventoryService inventoryService, AuditService auditService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
    }

    @Override
    public Order getByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    @Override
    public Order getById(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    public Order getByOrderNumberAndPhone(String orderNumber, String phone) {
        return orderRepository.findByOrderNumber(orderNumber)
                .filter(order -> Phones.matches(order.getCustomerPhone(), phone))
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    @Override
    public PageResponse<OrderSummaryResponse> searchAdmin(OrderStatus status, String q, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        return PageResponse.of(orderRepository.search(status, query, pageable).map(OrderSummaryResponse::from));
    }

    @Override
    @Transactional
    public Order changeStatus(UUID orderId, OrderStatus newStatus, UUID actingUserId) {
        Order order = getEntityById(orderId);
        applyTransition(order, newStatus, actingUserId);
        return order;
    }

    @Override
    @Transactional
    public Order cancel(UUID orderId, String reason, UUID actingUserId) {
        Order order = getEntityById(orderId);
        OrderStatus currentStatus = order.getOrderStatus();
        if (!CANCELLABLE_FROM.contains(currentStatus)) {
            throw new InvalidOrderStatusTransitionException(currentStatus, OrderStatus.CANCELLED);
        }
        order.changeStatus(OrderStatus.CANCELLED);
        restock(order, reason, actingUserId);
        auditService.record(AuditEventRequest.of(AuditAction.ORDER_CANCELLED, actingUserId, "Order", orderId));
        return order;
    }

    private void applyTransition(Order order, OrderStatus newStatus, UUID actingUserId) {
        OrderStatus currentStatus = order.getOrderStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(currentStatus, newStatus);
        }
        order.changeStatus(newStatus);
        if (restocksOnEntry(newStatus)) {
            restock(order, "Order " + newStatus.name().toLowerCase(), actingUserId);
        }
        auditService.record(AuditEventRequest.of(AuditAction.ORDER_STATUS_CHANGED, actingUserId, "Order", order.getId()));
    }

    /** Checkout already sold this stock (a {@code SALE} movement per item); give it back now that
     *  it's definitively not going to the customer. Each of {@code REJECTED}/{@code CANCELLED}/
     *  {@code RETURNED} is terminal in {@link #ALLOWED_TRANSITIONS} (no outgoing edges), so a given
     *  order can only reach one of them once — no double-restock risk. */
    private void restock(Order order, String reason, UUID actingUserId) {
        for (OrderItem item : orderItemRepository.findByOrderIdOrderById(order.getId())) {
            inventoryService.restock(item.getProductId(), item.getQuantity(), order.getId(), reason, actingUserId);
        }
    }

    private static boolean restocksOnEntry(OrderStatus status) {
        return status == OrderStatus.REJECTED || status == OrderStatus.CANCELLED || status == OrderStatus.RETURNED;
    }

    private Order getEntityById(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private static Map<OrderStatus, Set<OrderStatus>> allowedTransitions() {
        Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
        transitions.put(OrderStatus.PENDING_CONFIRMATION, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.REJECTED));
        transitions.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING));
        transitions.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_DELIVERY));
        transitions.put(OrderStatus.READY_FOR_DELIVERY, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY));
        transitions.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.FAILED_DELIVERY));
        transitions.put(OrderStatus.FAILED_DELIVERY, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.RETURNED));
        transitions.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURNED));
        transitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.RETURNED, EnumSet.noneOf(OrderStatus.class));
        return transitions;
    }
}
