package com.hosannasolutions.solisla.delivery.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.delivery.DeliveryZone;
import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneCreateRequest;
import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneUpdateRequest;
import com.hosannasolutions.solisla.delivery.exception.DeliveryZoneNotFoundException;
import com.hosannasolutions.solisla.delivery.repository.DeliveryZoneRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryZoneServiceImpl implements DeliveryZoneService {

    private final DeliveryZoneRepository deliveryZoneRepository;
    private final AuditService auditService;

    public DeliveryZoneServiceImpl(DeliveryZoneRepository deliveryZoneRepository, AuditService auditService) {
        this.deliveryZoneRepository = deliveryZoneRepository;
        this.auditService = auditService;
    }

    @Override
    public List<DeliveryZone> listActive() {
        return deliveryZoneRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    public List<DeliveryZone> listAll() {
        return deliveryZoneRepository.findAllByOrderByNameAsc();
    }

    @Override
    public DeliveryZone getById(UUID zoneId) {
        return deliveryZoneRepository.findById(zoneId).orElseThrow(() -> new DeliveryZoneNotFoundException(zoneId));
    }

    @Override
    public DeliveryZone getActiveById(UUID zoneId) {
        return deliveryZoneRepository.findById(zoneId)
                .filter(DeliveryZone::isActive)
                .orElseThrow(() -> new DeliveryZoneNotFoundException(zoneId));
    }

    @Override
    @Transactional
    public DeliveryZone create(DeliveryZoneCreateRequest request, UUID actingUserId) {
        DeliveryZone zone = deliveryZoneRepository.save(new DeliveryZone(request.name(), request.fee()));
        auditService.record(AuditEventRequest.of(AuditAction.DELIVERY_ZONE_CREATED, actingUserId, "DeliveryZone", zone.getId()));
        return zone;
    }

    @Override
    @Transactional
    public DeliveryZone update(UUID zoneId, DeliveryZoneUpdateRequest request, UUID actingUserId) {
        DeliveryZone zone = getById(zoneId);
        zone.update(request.name(), request.fee(), request.active());
        auditService.record(AuditEventRequest.of(AuditAction.DELIVERY_ZONE_UPDATED, actingUserId, "DeliveryZone", zoneId));
        return zone;
    }
}
