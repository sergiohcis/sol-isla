package com.hosannasolutions.solisla.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The central business object (design doc §17) — created once, atomically, by
 *  {@code CheckoutServiceImpl}, always starting {@code PENDING_CONFIRMATION}/{@code PENDING}.
 *  Nothing here is mutable yet: status-change methods land in Phase 6 alongside the admin
 *  endpoints that call them. */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_city", nullable = false)
    private String deliveryCity;

    @Column(name = "delivery_postal_code")
    private String deliveryPostalCode;

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    @Column(name = "delivery_zone_id", nullable = false)
    private UUID deliveryZoneId;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false)
    private BigDecimal discountTotal;

    @Column(name = "delivery_fee", nullable = false)
    private BigDecimal deliveryFee;

    @Column(name = "grand_total", nullable = false)
    private BigDecimal grandTotal;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(String orderNumber, String idempotencyKey, String customerName, String customerPhone,
                 String customerEmail, String deliveryAddress, String deliveryCity, String deliveryPostalCode,
                 String deliveryNotes, UUID deliveryZoneId, BigDecimal subtotal, BigDecimal discountTotal,
                 BigDecimal deliveryFee, BigDecimal grandTotal, String currency, PaymentMethod paymentMethod) {
        Instant now = Instant.now();
        this.orderNumber = orderNumber;
        this.idempotencyKey = idempotencyKey;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.deliveryAddress = deliveryAddress;
        this.deliveryCity = deliveryCity;
        this.deliveryPostalCode = deliveryPostalCode;
        this.deliveryNotes = deliveryNotes;
        this.deliveryZoneId = deliveryZoneId;
        this.subtotal = subtotal;
        this.discountTotal = discountTotal;
        this.deliveryFee = deliveryFee;
        this.grandTotal = grandTotal;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        // Both payment methods start PENDING (design doc §14) — CARD's real provider flow is Phase 7.
        this.paymentStatus = PaymentStatus.PENDING;
        this.orderStatus = OrderStatus.PENDING_CONFIRMATION;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public String getDeliveryPostalCode() {
        return deliveryPostalCode;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public UUID getDeliveryZoneId() {
        return deliveryZoneId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
