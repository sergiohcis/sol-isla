package com.hosannasolutions.solisla.catalog.dto;

import com.hosannasolutions.solisla.catalog.Product;
import com.hosannasolutions.solisla.catalog.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;

/** Lighter than {@link ProductResponse} — used for catalog listings so the payload doesn't carry
 *  every image and the full discount window for a grid of cards (design doc §11: paginate, don't
 *  return the entire catalog). */
public record ProductSummaryResponse(
        UUID id,
        String sku,
        String name,
        String slug,
        String primaryImageUrl,
        BigDecimal basePrice,
        BigDecimal effectivePrice,
        String currency,
        boolean featured,
        ProductStatus status,
        String categoryName
) {

    public static ProductSummaryResponse from(Product product, BigDecimal effectivePrice, String primaryImageUrl, String categoryName) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                primaryImageUrl,
                product.getBasePrice(),
                effectivePrice,
                product.getCurrency(),
                product.isFeatured(),
                product.getStatus(),
                categoryName);
    }
}
