package com.hosannasolutions.solisla.category.providedService;

import com.hosannasolutions.solisla.category.Category;
import com.hosannasolutions.solisla.category.dto.CategoryCreateRequest;
import com.hosannasolutions.solisla.category.dto.CategoryUpdateRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryService {

    List<Category> listActive();

    List<Category> listAll();

    Category getById(UUID categoryId);

    Optional<Category> findBySlug(String slug);

    Category create(CategoryCreateRequest request, UUID actingUserId);

    Category update(UUID categoryId, CategoryUpdateRequest request, UUID actingUserId);
}
