package com.hosannasolutions.solisla.catalog.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID productId) {
        super("Product not found: " + productId);
    }

    public ProductNotFoundException(String slug) {
        super("Product not found: " + slug);
    }
}
