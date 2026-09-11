package com.hosannasolutions.solisla.catalog.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.catalog.ProductImage;
import com.hosannasolutions.solisla.catalog.exception.ProductImageNotFoundException;
import com.hosannasolutions.solisla.catalog.repository.ProductImageRepository;
import com.hosannasolutions.solisla.common.storage.FileStorageService;
import com.hosannasolutions.solisla.common.storage.StoredFile;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository, ProductService productService,
                                    FileStorageService fileStorageService, AuditService auditService) {
        this.productImageRepository = productImageRepository;
        this.productService = productService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public ProductImage upload(UUID productId, MultipartFile file, String altText, UUID actingUserId) {
        productService.getEntityById(productId); // 404s if the product doesn't exist

        List<ProductImage> existing = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        StoredFile stored = fileStorageService.store("products/" + productId, file);
        ProductImage image = new ProductImage(
                productId, stored.publicUrl(), stored.storageKey(), altText, existing.size(), existing.isEmpty());
        image = productImageRepository.save(image);
        auditService.record(AuditEventRequest.of(AuditAction.PRODUCT_UPDATED, actingUserId, "Product", productId));
        return image;
    }

    @Override
    @Transactional
    public void delete(UUID imageId, UUID actingUserId) {
        ProductImage image = productImageRepository.findById(imageId).orElseThrow(() -> new ProductImageNotFoundException(imageId));
        UUID productId = image.getProductId();
        boolean wasPrimary = image.isPrimary();

        fileStorageService.delete(image.getStorageKey());
        productImageRepository.delete(image);

        if (wasPrimary) {
            productImageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setPrimary(true);
                        productImageRepository.save(next);
                    });
        }
        auditService.record(AuditEventRequest.of(AuditAction.PRODUCT_UPDATED, actingUserId, "Product", productId));
    }

    @Override
    @Transactional
    public void setPrimary(UUID imageId, UUID actingUserId) {
        ProductImage target = productImageRepository.findById(imageId).orElseThrow(() -> new ProductImageNotFoundException(imageId));
        List<ProductImage> siblings = productImageRepository.findByProductIdOrderBySortOrderAsc(target.getProductId());
        for (ProductImage sibling : siblings) {
            boolean shouldBePrimary = sibling.getId().equals(imageId);
            if (sibling.isPrimary() != shouldBePrimary) {
                sibling.setPrimary(shouldBePrimary);
                productImageRepository.save(sibling);
            }
        }
        auditService.record(AuditEventRequest.of(AuditAction.PRODUCT_UPDATED, actingUserId, "Product", target.getProductId()));
    }
}
