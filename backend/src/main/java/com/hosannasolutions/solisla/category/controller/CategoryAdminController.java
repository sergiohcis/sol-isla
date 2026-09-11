package com.hosannasolutions.solisla.category.controller;

import com.hosannasolutions.solisla.category.dto.CategoryCreateRequest;
import com.hosannasolutions.solisla.category.dto.CategoryResponse;
import com.hosannasolutions.solisla.category.dto.CategoryUpdateRequest;
import com.hosannasolutions.solisla.category.providedService.CategoryService;
import com.hosannasolutions.solisla.security.userdetails.SolIslaUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CategoryService categoryService;

    public CategoryAdminController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public List<CategoryResponse> list() {
        return categoryService.listAll().stream().map(CategoryResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    public CategoryResponse create(@Valid @RequestBody CategoryCreateRequest request,
                                    @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return CategoryResponse.from(categoryService.create(request, principal.getUserId()));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    public CategoryResponse update(@PathVariable UUID categoryId, @Valid @RequestBody CategoryUpdateRequest request,
                                    @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return CategoryResponse.from(categoryService.update(categoryId, request, principal.getUserId()));
    }
}
