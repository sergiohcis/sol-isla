package com.hosannasolutions.solisla.inventory.exception;

import java.util.UUID;

public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(UUID productId) {
        super("No inventory record for product: " + productId);
    }
}
