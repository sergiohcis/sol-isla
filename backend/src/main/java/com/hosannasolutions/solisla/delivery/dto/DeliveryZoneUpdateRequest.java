package com.hosannasolutions.solisla.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DeliveryZoneUpdateRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal fee,
        boolean active
) {
}
