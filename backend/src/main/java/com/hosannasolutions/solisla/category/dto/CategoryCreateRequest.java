package com.hosannasolutions.solisla.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoryCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        UUID parentId,
        int sortOrder
) {
}
