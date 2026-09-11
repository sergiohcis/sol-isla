package com.hosannasolutions.solisla.catalog.exception;

import com.hosannasolutions.solisla.catalog.ProductStatus;

public class InvalidProductStatusTransitionException extends RuntimeException {

    public InvalidProductStatusTransitionException(ProductStatus from, ProductStatus to) {
        super("Cannot transition product status from " + from + " to " + to);
    }
}
