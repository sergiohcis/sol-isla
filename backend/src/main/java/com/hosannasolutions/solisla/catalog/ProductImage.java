package com.hosannasolutions.solisla.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProductImage() {
    }

    public ProductImage(UUID productId, String url, String storageKey, String altText, int sortOrder, boolean primary) {
        this.productId = productId;
        this.url = url;
        this.storageKey = storageKey;
        this.altText = altText;
        this.sortOrder = sortOrder;
        this.primary = primary;
        this.createdAt = Instant.now();
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getUrl() {
        return url;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getAltText() {
        return altText;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
