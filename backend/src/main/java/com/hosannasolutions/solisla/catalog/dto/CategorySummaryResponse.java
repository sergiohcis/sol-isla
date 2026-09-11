package com.hosannasolutions.solisla.catalog.dto;

import com.hosannasolutions.solisla.category.Category;
import java.util.UUID;

public record CategorySummaryResponse(UUID id, String name, String slug) {

    public static CategorySummaryResponse from(Category category) {
        return new CategorySummaryResponse(category.getId(), category.getName(), category.getSlug());
    }
}
