package com.hosannasolutions.solisla.catalog.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.catalog.Product;
import com.hosannasolutions.solisla.catalog.ProductImage;
import com.hosannasolutions.solisla.catalog.ProductStatus;
import com.hosannasolutions.solisla.catalog.dto.CategorySummaryResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductCreateRequest;
import com.hosannasolutions.solisla.catalog.dto.ProductImageResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductSummaryResponse;
import com.hosannasolutions.solisla.catalog.dto.ProductUpdateRequest;
import com.hosannasolutions.solisla.catalog.exception.DuplicateSkuException;
import com.hosannasolutions.solisla.catalog.exception.InvalidProductStatusTransitionException;
import com.hosannasolutions.solisla.catalog.exception.ProductNotFoundException;
import com.hosannasolutions.solisla.catalog.pricing.ProductPricingService;
import com.hosannasolutions.solisla.catalog.repository.ProductImageRepository;
import com.hosannasolutions.solisla.catalog.repository.ProductRepository;
import com.hosannasolutions.solisla.category.Category;
import com.hosannasolutions.solisla.category.providedService.CategoryService;
import com.hosannasolutions.solisla.common.api.PageResponse;
import com.hosannasolutions.solisla.common.util.Slugs;
import com.hosannasolutions.solisla.inventory.providedService.InventoryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED_TRANSITIONS = allowedTransitions();

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;
    private final ProductPricingService pricingService;
    private final AuditService auditService;

    public ProductServiceImpl(ProductRepository productRepository, ProductImageRepository productImageRepository,
                               CategoryService categoryService, InventoryService inventoryService,
                               ProductPricingService pricingService, AuditService auditService) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryService = categoryService;
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
        this.auditService = auditService;
    }

    @Override
    public PageResponse<ProductSummaryResponse> searchPublic(String q, String categorySlug, BigDecimal minPrice,
                                                               BigDecimal maxPrice, Boolean featured, Pageable pageable) {
        UUID categoryId = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            var category = categoryService.findBySlug(categorySlug);
            if (category.isEmpty()) {
                return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
            }
            categoryId = category.get().getId();
        }
        Page<Product> page = productRepository.search(ProductStatus.ACTIVE, categoryId, blankToEmpty(q), minPrice, maxPrice, featured, pageable);
        return toSummaryPage(page);
    }

    @Override
    public ProductResponse getPublicBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ProductNotFoundException(slug));
        return toResponse(product);
    }

    @Override
    public PageResponse<ProductSummaryResponse> searchAdmin(String q, UUID categoryId, ProductStatus status, Boolean featured,
                                                              Pageable pageable) {
        Page<Product> page = productRepository.search(status, categoryId, blankToEmpty(q), null, null, featured, pageable);
        return toSummaryPage(page);
    }

    @Override
    public ProductResponse getAdminResponseById(UUID productId) {
        return toResponse(getEntityById(productId));
    }

    @Override
    public Product getEntityById(UUID productId) {
        return productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    @Transactional
    public Product create(ProductCreateRequest request, UUID actingUserId) {
        requireCategoryExists(request.categoryId());
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        String slug = uniqueSlug(request.name(), null);
        Product product = new Product(
                request.sku(), request.name(), slug, request.description(), request.categoryId(), request.basePrice(),
                request.currency(), request.discountType(), request.discountValue(), request.discountEffectiveFrom(),
                request.discountEffectiveTo(), request.featured());
        product = productRepository.save(product);
        inventoryService.initializeForProduct(product.getId(), request.initialStockQuantity(), actingUserId);
        auditService.record(AuditEventRequest.of(AuditAction.PRODUCT_CREATED, actingUserId, "Product", product.getId()));
        return product;
    }

    @Override
    @Transactional
    public Product update(UUID productId, ProductUpdateRequest request, UUID actingUserId) {
        Product product = getEntityById(productId);
        requireCategoryExists(request.categoryId());
        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        String slug = product.getName().equals(request.name()) ? product.getSlug() : uniqueSlug(request.name(), productId);
        product.update(
                request.sku(), request.name(), slug, request.description(), request.categoryId(), request.basePrice(),
                request.currency(), request.discountType(), request.discountValue(), request.discountEffectiveFrom(),
                request.discountEffectiveTo(), request.featured());
        auditService.record(AuditEventRequest.of(AuditAction.PRODUCT_UPDATED, actingUserId, "Product", productId));
        return product;
    }

    @Override
    @Transactional
    public Product changeStatus(UUID productId, ProductStatus newStatus, UUID actingUserId) {
        Product product = getEntityById(productId);
        ProductStatus currentStatus = product.getStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new InvalidProductStatusTransitionException(currentStatus, newStatus);
        }
        product.changeStatus(newStatus);
        AuditAction action = newStatus == ProductStatus.ARCHIVED ? AuditAction.PRODUCT_ARCHIVED : AuditAction.PRODUCT_STATUS_CHANGED;
        auditService.record(AuditEventRequest.of(action, actingUserId, "Product", productId));
        return product;
    }

    @Override
    public ProductResponse toResponse(Product product) {
        Category category = categoryService.getById(product.getCategoryId());
        List<ProductImageResponse> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream()
                .map(ProductImageResponse::from)
                .toList();
        BigDecimal effectivePrice = pricingService.effectivePrice(product, Instant.now());
        int stockQuantity = inventoryService.availableQuantityOrZero(product.getId());
        return ProductResponse.from(product, CategorySummaryResponse.from(category), effectivePrice, stockQuantity, images);
    }

    private PageResponse<ProductSummaryResponse> toSummaryPage(Page<Product> page) {
        List<Product> products = page.getContent();
        List<UUID> productIds = products.stream().map(Product::getId).toList();

        Map<UUID, Category> categoriesById = categoryService.listAll().stream()
                .collect(java.util.stream.Collectors.toMap(Category::getId, c -> c));
        Map<UUID, String> primaryImageByProductId = productImageRepository.findByProductIdInOrderBySortOrderAsc(productIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(ProductImage::getProductId,
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(),
                                ProductServiceImpl::firstDisplayableImageUrl)));
        Instant now = Instant.now();

        List<ProductSummaryResponse> summaries = products.stream()
                .map(product -> {
                    Category category = categoriesById.get(product.getCategoryId());
                    BigDecimal effectivePrice = pricingService.effectivePrice(product, now);
                    return ProductSummaryResponse.from(
                            product, effectivePrice, primaryImageByProductId.get(product.getId()),
                            category != null ? category.getName() : null);
                })
                .toList();

        return PageResponse.of(page, summaries);
    }

    private void requireCategoryExists(UUID categoryId) {
        categoryService.getById(categoryId);
    }

    private String uniqueSlug(String name, UUID excludingProductId) {
        String base = Slugs.slugify(name);
        String candidate = base;
        int suffix = 2;
        while (isSlugTaken(candidate, excludingProductId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, UUID excludingProductId) {
        return productRepository.findBySlug(slug)
                .map(existing -> !existing.getId().equals(excludingProductId))
                .orElse(false);
    }

    /** The image list is already ordered by sort_order (repository query) — prefer the one
     *  flagged primary, otherwise fall back to the first in that order. */
    private static String firstDisplayableImageUrl(List<ProductImage> images) {
        return images.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> images.stream().findFirst())
                .map(ProductImage::getUrl)
                .orElse(null);
    }

    /** Never null — see the {@code q = ''} bind-type note on {@code ProductRepository.search}. */
    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static Map<ProductStatus, Set<ProductStatus>> allowedTransitions() {
        Map<ProductStatus, Set<ProductStatus>> transitions = new EnumMap<>(ProductStatus.class);
        transitions.put(ProductStatus.DRAFT, EnumSet.of(ProductStatus.ACTIVE, ProductStatus.ARCHIVED));
        transitions.put(ProductStatus.ACTIVE, EnumSet.of(ProductStatus.INACTIVE, ProductStatus.ARCHIVED));
        transitions.put(ProductStatus.INACTIVE, EnumSet.of(ProductStatus.ACTIVE, ProductStatus.ARCHIVED));
        transitions.put(ProductStatus.ARCHIVED, EnumSet.noneOf(ProductStatus.class));
        return transitions;
    }
}
