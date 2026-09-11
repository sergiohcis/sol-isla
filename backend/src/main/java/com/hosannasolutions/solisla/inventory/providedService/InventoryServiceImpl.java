package com.hosannasolutions.solisla.inventory.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.inventory.Inventory;
import com.hosannasolutions.solisla.inventory.InventoryMovement;
import com.hosannasolutions.solisla.inventory.InventoryMovementType;
import com.hosannasolutions.solisla.inventory.exception.InsufficientStockException;
import com.hosannasolutions.solisla.inventory.exception.InventoryNotFoundException;
import com.hosannasolutions.solisla.inventory.repository.InventoryMovementRepository;
import com.hosannasolutions.solisla.inventory.repository.InventoryRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final AuditService auditService;

    public InventoryServiceImpl(InventoryRepository inventoryRepository, InventoryMovementRepository movementRepository,
                                 AuditService auditService) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public Inventory initializeForProduct(UUID productId, int initialQuantity, UUID actingUserId) {
        Inventory inventory = inventoryRepository.save(new Inventory(productId, initialQuantity));
        if (initialQuantity != 0) {
            movementRepository.save(new InventoryMovement(
                    productId, InventoryMovementType.PURCHASE, initialQuantity, "Product", productId, "Initial stock", actingUserId));
        }
        return inventory;
    }

    @Override
    public Inventory getByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId).orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    @Override
    public int availableQuantityOrZero(UUID productId) {
        return inventoryRepository.findByProductId(productId).map(Inventory::getAvailableQuantity).orElse(0);
    }

    @Override
    public Map<UUID, Integer> availableQuantitiesOrZero(List<UUID> productIds) {
        return inventoryRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventory::getProductId, Inventory::getAvailableQuantity));
    }

    @Override
    @Transactional
    public Inventory adjust(UUID productId, int delta, String reason, UUID actingUserId) {
        if (delta == 0) {
            throw new IllegalArgumentException("Adjustment delta must not be zero");
        }
        Inventory inventory = getByProductId(productId);
        try {
            inventory.applyDelta(delta);
        } catch (IllegalStateException e) {
            throw new InsufficientStockException(
                    "Cannot adjust stock by " + delta + ": only " + inventory.getAvailableQuantity() + " available");
        }
        movementRepository.save(new InventoryMovement(
                productId, InventoryMovementType.ADJUSTMENT, delta, "Product", productId, reason, actingUserId));
        auditService.record(AuditEventRequest.of(AuditAction.INVENTORY_ADJUSTED, actingUserId, "Product", productId));
        return inventory;
    }

    @Override
    @Transactional
    public Inventory sell(UUID productId, int quantity, UUID orderId, UUID actingUserId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Sale quantity must be positive");
        }
        Inventory inventory = getByProductId(productId);
        try {
            inventory.applyDelta(-quantity);
        } catch (IllegalStateException e) {
            throw new InsufficientStockException(
                    "Cannot sell " + quantity + ": only " + inventory.getAvailableQuantity() + " available");
        }
        movementRepository.save(new InventoryMovement(
                productId, InventoryMovementType.SALE, -quantity, "Order", orderId, "Checkout", actingUserId));
        return inventory;
    }
}
