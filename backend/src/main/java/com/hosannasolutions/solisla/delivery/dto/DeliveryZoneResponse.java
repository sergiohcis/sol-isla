package com.hosannasolutions.solisla.delivery.dto;

import com.hosannasolutions.solisla.delivery.DeliveryZone;
import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryZoneResponse(UUID id, String name, BigDecimal fee, boolean active) {

    public static DeliveryZoneResponse from(DeliveryZone zone) {
        return new DeliveryZoneResponse(zone.getId(), zone.getName(), zone.getFee(), zone.isActive());
    }
}
