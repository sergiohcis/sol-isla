package com.hosannasolutions.solisla.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code version} backs optimistic locking (JPA {@link Version}) so two concurrent admin edits
 * can't silently clobber each other — same reasoning CLAUDE.md rule 4 calls out for inventory.
 * <p>
 * {@code categoryId} is a plain column, not a JPA association — same style as
 * {@code Category#parentId} — the catalog service resolves category data explicitly when
 * assembling a response rather than relying on lazy-loading.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "discount_effective_from")
    private Instant discountEffectiveFrom;

    @Column(name = "discount_effective_to")
    private Instant discountEffectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Product() {
    }

    public Product(String sku, String name, String slug, String description, UUID categoryId, BigDecimal basePrice,
                    String currency, DiscountType discountType, BigDecimal discountValue, Instant discountEffectiveFrom,
                    Instant discountEffectiveTo, boolean featured) {
        Instant now = Instant.now();
        this.sku = sku;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.currency = currency;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.discountEffectiveFrom = discountEffectiveFrom;
        this.discountEffectiveTo = discountEffectiveTo;
        this.status = ProductStatus.DRAFT;
        this.featured = featured;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String sku, String name, String slug, String description, UUID categoryId, BigDecimal basePrice,
                        String currency, DiscountType discountType, BigDecimal discountValue, Instant discountEffectiveFrom,
                        Instant discountEffectiveTo, boolean featured) {
        this.sku = sku;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.currency = currency;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.discountEffectiveFrom = discountEffectiveFrom;
        this.discountEffectiveTo = discountEffectiveTo;
        this.featured = featured;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public String getCurrency() {
        return currency;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public Instant getDiscountEffectiveFrom() {
        return discountEffectiveFrom;
    }

    public Instant getDiscountEffectiveTo() {
        return discountEffectiveTo;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
