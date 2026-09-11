package com.hosannasolutions.solisla.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Immutable price/product snapshot (design doc §9 / CLAUDE.md rule 3) — a later product rename
 *  or price change must never alter what a past order shows. No setters; once written by
 *  checkout, an OrderItem never changes. */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_snapshot", nullable = false)
    private String skuSnapshot;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "final_unit_price", nullable = false)
    private BigDecimal finalUnitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    public OrderItem(UUID orderId, UUID productId, String skuSnapshot, String productNameSnapshot,
                      BigDecimal unitPrice, BigDecimal discountAmount, BigDecimal finalUnitPrice, int quantity,
                      BigDecimal lineTotal) {
        this.orderId = orderId;
        this.productId = productId;
        this.skuSnapshot = skuSnapshot;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPrice = unitPrice;
        this.discountAmount = discountAmount;
        this.finalUnitPrice = finalUnitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSkuSnapshot() {
        return skuSnapshot;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getFinalUnitPrice() {
        return finalUnitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
