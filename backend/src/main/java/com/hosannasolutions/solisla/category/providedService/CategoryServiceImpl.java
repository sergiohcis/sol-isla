package com.hosannasolutions.solisla.category.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.category.Category;
import com.hosannasolutions.solisla.category.dto.CategoryCreateRequest;
import com.hosannasolutions.solisla.category.dto.CategoryUpdateRequest;
import com.hosannasolutions.solisla.category.exception.CategoryNotFoundException;
import com.hosannasolutions.solisla.category.repository.CategoryRepository;
import com.hosannasolutions.solisla.common.util.Slugs;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    @Override
    public List<Category> listActive() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Override
    public List<Category> listAll() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    @Override
    public Category getById(UUID categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    @Override
    @Transactional
    public Category create(CategoryCreateRequest request, UUID actingUserId) {
        String slug = uniqueSlug(request.name(), null);
        Category category = new Category(request.name(), slug, request.description(), request.parentId(), request.sortOrder());
        category = categoryRepository.save(category);
        auditService.record(AuditEventRequest.of(AuditAction.CATEGORY_CREATED, actingUserId, "Category", category.getId()));
        return category;
    }

    @Override
    @Transactional
    public Category update(UUID categoryId, CategoryUpdateRequest request, UUID actingUserId) {
        Category category = getById(categoryId);
        String slug = category.getName().equals(request.name()) ? category.getSlug() : uniqueSlug(request.name(), categoryId);
        category.update(request.name(), slug, request.description(), request.parentId(), request.sortOrder());
        category.setActive(request.active());
        auditService.record(AuditEventRequest.of(AuditAction.CATEGORY_UPDATED, actingUserId, "Category", categoryId));
        return category;
    }

    private String uniqueSlug(String name, UUID excludingCategoryId) {
        String base = Slugs.slugify(name);
        String candidate = base;
        int suffix = 2;
        while (isSlugTaken(candidate, excludingCategoryId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, UUID excludingCategoryId) {
        return categoryRepository.findBySlug(slug)
                .map(existing -> !existing.getId().equals(excludingCategoryId))
                .orElse(false);
    }
}
