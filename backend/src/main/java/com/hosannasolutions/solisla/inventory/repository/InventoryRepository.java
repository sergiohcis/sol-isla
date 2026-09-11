package com.hosannasolutions.solisla.inventory.repository;

import com.hosannasolutions.solisla.inventory.Inventory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    List<Inventory> findByProductIdIn(List<UUID> productIds);
}
