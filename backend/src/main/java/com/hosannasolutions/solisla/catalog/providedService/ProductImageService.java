package com.hosannasolutions.solisla.catalog.providedService;

import com.hosannasolutions.solisla.catalog.ProductImage;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageService {

    ProductImage upload(UUID productId, MultipartFile file, String altText, UUID actingUserId);

    void delete(UUID imageId, UUID actingUserId);

    void setPrimary(UUID imageId, UUID actingUserId);
}
