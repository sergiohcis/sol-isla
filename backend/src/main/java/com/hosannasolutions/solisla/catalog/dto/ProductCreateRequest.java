package com.hosannasolutions.solisla.catalog.dto;

import com.hosannasolutions.solisla.catalog.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductCreateRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 255) String name,
        @NotNull UUID categoryId,
        String description,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal basePrice,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull DiscountType discountType,
        BigDecimal discountValue,
        Instant discountEffectiveFrom,
        Instant discountEffectiveTo,
        boolean featured,
        @Min(0) int initialStockQuantity
) {
}
