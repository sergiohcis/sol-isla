package com.hosannasolutions.solisla.catalog.dto;

import com.hosannasolutions.solisla.catalog.DiscountType;
import com.hosannasolutions.solisla.catalog.Product;
import com.hosannasolutions.solisla.catalog.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String slug,
        String description,
        CategorySummaryResponse category,
        BigDecimal basePrice,
        String currency,
        DiscountType discountType,
        BigDecimal discountValue,
        Instant discountEffectiveFrom,
        Instant discountEffectiveTo,
        BigDecimal effectivePrice,
        ProductStatus status,
        boolean featured,
        int stockQuantity,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductResponse from(Product product, CategorySummaryResponse category, BigDecimal effectivePrice,
                                        int stockQuantity, List<ProductImageResponse> images) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                category,
                product.getBasePrice(),
                product.getCurrency(),
                product.getDiscountType(),
                product.getDiscountValue(),
                product.getDiscountEffectiveFrom(),
                product.getDiscountEffectiveTo(),
                effectivePrice,
                product.getStatus(),
                product.isFeatured(),
                stockQuantity,
                images,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
