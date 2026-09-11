package com.hosannasolutions.solisla.catalog.exception;

import java.util.UUID;

public class ProductImageNotFoundException extends RuntimeException {

    public ProductImageNotFoundException(UUID imageId) {
        super("Product image not found: " + imageId);
    }
}
