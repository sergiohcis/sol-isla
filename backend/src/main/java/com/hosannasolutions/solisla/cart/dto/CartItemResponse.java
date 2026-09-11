package com.hosannasolutions.solisla.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        String primaryImageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int availableStock
) {
}
