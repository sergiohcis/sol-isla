package com.hosannasolutions.solisla.catalog.exception;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("SKU already in use: " + sku);
    }
}
