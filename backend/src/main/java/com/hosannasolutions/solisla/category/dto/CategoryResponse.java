package com.hosannasolutions.solisla.category.dto;

import com.hosannasolutions.solisla.category.Category;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        UUID parentId,
        boolean active,
        int sortOrder
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getParentId(),
                category.isActive(),
                category.getSortOrder());
    }
}
