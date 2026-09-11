package com.hosannasolutions.solisla.catalog.providedService;

import com.hosannasolutions.solisla.catalog.Product;
import com.hosannasolutions.solisla.catalog.ProductStatus;
import com.hosannasolutions.solisla.catalog.dto.ProductCreateRequest;
import com.hosannasolutions.solisla.catalog.dto.ProductResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductSummaryResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductUpdateRequest;
import com.hosannasolutions.solisla.common.api.PageResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    /** Storefront search — always restricted to {@link ProductStatus#ACTIVE} regardless of what
     *  the admin search allows (design doc §11). */
    PageResponse<ProductSummaryResponse> searchPublic(String q, String categorySlug, BigDecimal minPrice,
                                                        BigDecimal maxPrice, Boolean featured, Pageable pageable);

    ProductResponse getPublicBySlug(String slug);

    PageResponse<ProductSummaryResponse> searchAdmin(String q, UUID categoryId, ProductStatus status, Boolean featured,
                                                       Pageable pageable);

    ProductResponse getAdminResponseById(UUID productId);

    Product getEntityById(UUID productId);

    Product create(ProductCreateRequest request, UUID actingUserId);

    Product update(UUID productId, ProductUpdateRequest request, UUID actingUserId);

    Product changeStatus(UUID productId, ProductStatus newStatus, UUID actingUserId);

    ProductResponse toResponse(Product product);
}
