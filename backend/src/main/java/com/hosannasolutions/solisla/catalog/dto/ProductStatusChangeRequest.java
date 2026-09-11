package com.hosannasolutions.solisla.catalog.dto;

import com.hosannasolutions.solisla.catalog.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusChangeRequest(@NotNull ProductStatus status) {
}
