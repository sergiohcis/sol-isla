package com.hosannasolutions.solisla.catalog.dto;

import com.hosannasolutions.solisla.catalog.ProductImage;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String url,
        String altText,
        int sortOrder,
        boolean primary
) {

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(image.getId(), image.getUrl(), image.getAltText(), image.getSortOrder(), image.isPrimary());
    }
}
