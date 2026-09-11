package com.hosannasolutions.solisla.category.controller;

import com.hosannasolutions.solisla.category.dto.CategoryResponse;
import com.hosannasolutions.solisla.category.providedService.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only, active categories only — see {@code CategoryAdminController} for the
 *  authenticated CRUD surface. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.listActive().stream().map(CategoryResponse::from).toList();
    }
}
