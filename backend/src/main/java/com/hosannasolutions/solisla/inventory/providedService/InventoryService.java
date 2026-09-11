package com.hosannasolutions.solisla.inventory.providedService;

import com.hosannasolutions.solisla.inventory.Inventory;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InventoryService {

    /** Called within the same transaction as product creation (design doc §36/CLAUDE.md rule 4:
     *  never just a counter — the initial stock is itself a PURCHASE movement). */
    Inventory initializeForProduct(UUID productId, int initialQuantity, UUID actingUserId);

    Inventory getByProductId(UUID productId);

    /** 0 for a product with no inventory record yet, rather than throwing — used when assembling
     *  read-only catalog responses where a missing row shouldn't break the listing. */
    int availableQuantityOrZero(UUID productId);

    Map<UUID, Integer> availableQuantitiesOrZero(List<UUID> productIds);

    Inventory adjust(UUID productId, int delta, String reason, UUID actingUserId);

    /** Called from within the checkout transaction (design doc §19 step 12) — decrements stock
     *  and records a SALE movement referencing the order. Optimistic locking on {@code Inventory}
     *  means a concurrent sale of the last unit surfaces as an
     *  {@code ObjectOptimisticLockingFailureException} here, letting the whole checkout
     *  transaction roll back rather than silently overselling (CLAUDE.md rule 4). */
    Inventory sell(UUID productId, int quantity, UUID orderId, UUID actingUserId);
}
