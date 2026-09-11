package com.hosannasolutions.solisla.inventory.dto;

import com.hosannasolutions.solisla.inventory.Inventory;
import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(UUID productId, int availableQuantity, int reservedQuantity, Instant updatedAt) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getProductId(), inventory.getAvailableQuantity(), inventory.getReservedQuantity(), inventory.getUpdatedAt());
    }
}
