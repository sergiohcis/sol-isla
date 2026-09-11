package com.hosannasolutions.solisla.catalog.controller;

import com.hosannasolutions.solisla.catalog.ProductStatus;
import com.hosannasolutions.solisla.catalog.dto.ProductCreateRequest;
import com.hosannasolutions.solisla.catalog.dto.ProductImageResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductStatusChangeRequest;
import com.hosannasolutions.solisla.catalog.dto.ProductSummaryResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductUpdateRequest;
import com.hosannasolutions.solisla.catalog.providedService.ProductImageService;
import com.hosannasolutions.solisla.catalog.providedService.ProductService;
import com.hosannasolutions.solisla.common.api.PageResponse;
import com.hosannasolutions.solisla.security.userdetails.SolIslaUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products")
public class ProductAdminController {

    private final ProductService productService;
    private final ProductImageService productImageService;

    public ProductAdminController(ProductService productService, ProductImageService productImageService) {
        this.productService = productService;
        this.productImageService = productImageService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    public PageResponse<ProductSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Boolean featured,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return productService.searchAdmin(q, categoryId, status, featured, pageable);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    public ProductResponse get(@PathVariable UUID productId) {
        return productService.getAdminResponseById(productId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request,
                                   @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return productService.toResponse(productService.create(request, principal.getUserId()));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ProductResponse update(@PathVariable UUID productId, @Valid @RequestBody ProductUpdateRequest request,
                                   @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return productService.toResponse(productService.update(productId, request, principal.getUserId()));
    }

    @PatchMapping("/{productId}/status")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ProductResponse changeStatus(@PathVariable UUID productId, @Valid @RequestBody ProductStatusChangeRequest request,
                                         @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return productService.toResponse(productService.changeStatus(productId, request.status(), principal.getUserId()));
    }

    @PostMapping(value = "/{productId}/images", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImageResponse uploadImage(@PathVariable UUID productId, @RequestPart MultipartFile file,
                                             @RequestPart(required = false) String altText,
                                             @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return ProductImageResponse.from(productImageService.upload(productId, file, altText, principal.getUserId()));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable UUID productId, @PathVariable UUID imageId,
                             @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        productImageService.delete(imageId, principal.getUserId());
    }

    @PostMapping("/{productId}/images/{imageId}/primary")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public void setPrimaryImage(@PathVariable UUID productId, @PathVariable UUID imageId,
                                 @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        productImageService.setPrimary(imageId, principal.getUserId());
    }
}
