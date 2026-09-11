package com.hosannasolutions.solisla.catalog.controller;

import com.hosannasolutions.solisla.catalog.dto.ProductResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductSummaryResponse;
import com.hosannasolutions.solisla.catalog.providedService.ProductService;
import com.hosannasolutions.solisla.common.api.PageResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public storefront catalog (design doc §11/§33) — always restricted to ACTIVE products; see
 *  {@code ProductAdminController} for the authenticated management surface. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean featured,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return productService.searchPublic(q, category, minPrice, maxPrice, featured, pageable);
    }

    @GetMapping("/{slug}")
    public ProductResponse getBySlug(@PathVariable String slug) {
        return productService.getPublicBySlug(slug);
    }
}
