package com.hosannasolutions.solisla.delivery.repository;

import com.hosannasolutions.solisla.delivery.DeliveryZone;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {

    List<DeliveryZone> findByActiveTrueOrderByNameAsc();

    List<DeliveryZone> findAllByOrderByNameAsc();
}
