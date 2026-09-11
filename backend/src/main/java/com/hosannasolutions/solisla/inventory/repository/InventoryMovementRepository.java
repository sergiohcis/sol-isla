package com.hosannasolutions.solisla.inventory.repository;

import com.hosannasolutions.solisla.inventory.InventoryMovement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(UUID productId);
}
