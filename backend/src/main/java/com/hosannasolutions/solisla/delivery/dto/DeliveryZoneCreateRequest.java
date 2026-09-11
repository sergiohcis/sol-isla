package com.hosannasolutions.solisla.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DeliveryZoneCreateRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal fee
) {
}
