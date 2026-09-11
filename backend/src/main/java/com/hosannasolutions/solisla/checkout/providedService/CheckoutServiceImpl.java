package com.hosannasolutions.solisla.checkout.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.cart.Cart;
import com.hosannasolutions.solisla.cart.CartItem;
import com.hosannasolutions.solisla.cart.CartStatus;
import com.hosannasolutions.solisla.cart.exception.InvalidCartQuantityException;
import com.hosannasolutions.solisla.cart.exception.ProductNotAvailableException;
import com.hosannasolutions.solisla.cart.repository.CartItemRepository;
import com.hosannasolutions.solisla.cart.repository.CartRepository;
import com.hosannasolutions.solisla.catalog.Product;
import com.hosannasolutions.solisla.catalog.ProductStatus;
import com.hosannasolutions.solisla.catalog.exception.ProductNotFoundException;
import com.hosannasolutions.solisla.catalog.pricing.ProductPricingService;
import com.hosannasolutions.solisla.catalog.repository.ProductRepository;
import com.hosannasolutions.solisla.checkout.dto.CheckoutRequest;
import com.hosannasolutions.solisla.checkout.exception.EmptyCartException;
import com.hosannasolutions.solisla.delivery.DeliveryZone;
import com.hosannasolutions.solisla.delivery.providedService.DeliveryZoneService;
import com.hosannasolutions.solisla.inventory.providedService.InventoryService;
import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.OrderItem;
import com.hosannasolutions.solisla.order.dto.OrderItemResponse;
import com.hosannasolutions.solisla.order.dto.OrderResponse;
import com.hosannasolutions.solisla.order.repository.OrderItemRepository;
import com.hosannasolutions.solisla.order.repository.OrderRepository;
import com.hosannasolutions.solisla.whatsapp.MessageOutbox;
import com.hosannasolutions.solisla.whatsapp.repository.MessageOutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The most important service in this codebase (see {@code checkout/package-info.java}):
 * idempotency check -> load cart -> server-side pricing recalculation -> inventory decrement ->
 * order creation -> outbox enqueue, all inside one {@code @Transactional} boundary. No external
 * HTTP call happens here — the WhatsApp send itself is Phase 5, running from the outbox row this
 * writes (CLAUDE.md rule 6).
 */
@Service
public class CheckoutServiceImpl implements CheckoutService {

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductPricingService pricingService;
    private final InventoryService inventoryService;
    private final DeliveryZoneService deliveryZoneService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MessageOutboxRepository messageOutboxRepository;
    private final AuditService auditService;

    public CheckoutServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
                                ProductRepository productRepository, ProductPricingService pricingService,
                                InventoryService inventoryService, DeliveryZoneService deliveryZoneService,
                                OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                                MessageOutboxRepository messageOutboxRepository, AuditService auditService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.pricingService = pricingService;
        this.inventoryService = inventoryService;
        this.deliveryZoneService = deliveryZoneService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.messageOutboxRepository = messageOutboxRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public OrderResponse checkout(String cartSessionToken, CheckoutRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Cart cart = loadNonEmptyCart(cartSessionToken);
        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        DeliveryZone deliveryZone = deliveryZoneService.getActiveById(request.deliveryZoneId());

        List<UUID> productIds = cartItems.stream().map(CartItem::getProductId).toList();
        Map<UUID, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Instant now = Instant.now();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        List<PricedLine> pricedLines = new ArrayList<>(cartItems.size());

        for (CartItem cartItem : cartItems) {
            Product product = productsById.get(cartItem.getProductId());
            if (product == null) {
                throw new ProductNotFoundException(cartItem.getProductId());
            }
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new ProductNotAvailableException(product.getId());
            }
            int availableStock = inventoryService.availableQuantityOrZero(product.getId());
            if (cartItem.getQuantity() > availableStock) {
                throw new InvalidCartQuantityException(
                        "Only " + availableStock + " of \"" + product.getName() + "\" in stock");
            }

            BigDecimal unitPrice = product.getBasePrice();
            BigDecimal finalUnitPrice = pricingService.effectivePrice(product, now);
            BigDecimal discountAmount = unitPrice.subtract(finalUnitPrice);
            BigDecimal lineTotal = finalUnitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            subtotal = subtotal.add(lineTotal);
            discountTotal = discountTotal.add(discountAmount.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            pricedLines.add(new PricedLine(product, cartItem.getQuantity(), unitPrice, discountAmount, finalUnitPrice, lineTotal));
        }

        BigDecimal deliveryFee = deliveryZone.getFee();
        BigDecimal grandTotal = subtotal.add(deliveryFee);

        Order order = new Order(
                generateOrderNumber(), blankToNull(idempotencyKey), request.customerName(), request.customerPhone(),
                request.customerEmail(), request.deliveryAddress(), request.deliveryCity(), request.deliveryPostalCode(),
                request.deliveryNotes(), deliveryZone.getId(), subtotal, discountTotal, deliveryFee, grandTotal,
                cart.getCurrency(), request.paymentMethod());
        order = orderRepository.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>(pricedLines.size());
        for (PricedLine line : pricedLines) {
            OrderItem orderItem = new OrderItem(
                    order.getId(), line.product().getId(), line.product().getSku(), line.product().getName(),
                    line.unitPrice(), line.discountAmount(), line.finalUnitPrice(), line.quantity(), line.lineTotal());
            orderItemRepository.save(orderItem);
            itemResponses.add(OrderItemResponse.from(orderItem));

            // Optimistic locking on Inventory means a concurrent sale of the last unit surfaces
            // here as an ObjectOptimisticLockingFailureException, rolling back the whole
            // transaction rather than letting two customers both "win" the last item.
            inventoryService.sell(line.product().getId(), line.quantity(), order.getId(), null);
        }

        cart.markConverted();
        cartRepository.save(cart);

        messageOutboxRepository.save(new MessageOutbox(
                "Order", order.getId(), "ORDER_CREATED", "{\"orderId\":\"" + order.getId() + "\"}"));
        auditService.record(AuditEventRequest.of(AuditAction.ORDER_CREATED, null, "Order", order.getId()));

        return OrderResponse.from(order, itemResponses);
    }

    private Cart loadNonEmptyCart(String cartSessionToken) {
        if (cartSessionToken == null || cartSessionToken.isBlank()) {
            throw new EmptyCartException();
        }
        Cart cart = cartRepository.findBySessionTokenAndStatus(cartSessionToken, CartStatus.ACTIVE)
                .filter(c -> !c.isExpired())
                .orElseThrow(EmptyCartException::new);
        if (cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId()).isEmpty()) {
            throw new EmptyCartException();
        }
        return cart;
    }

    private String generateOrderNumber() {
        long sequence = orderRepository.nextOrderNumberSequence();
        return "ORD-" + ORDER_NUMBER_DATE_FORMAT.format(Instant.now()) + "-" + String.format("%06d", sequence);
    }

    private OrderResponse toResponse(Order order) {
        var items = orderItemRepository.findByOrderIdOrderById(order.getId()).stream().map(OrderItemResponse::from).toList();
        return OrderResponse.from(order, items);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record PricedLine(Product product, int quantity, BigDecimal unitPrice, BigDecimal discountAmount,
                               BigDecimal finalUnitPrice, BigDecimal lineTotal) {
    }
}
