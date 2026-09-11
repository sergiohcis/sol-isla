package com.hosannasolutions.solisla.delivery.providedService;

import com.hosannasolutions.solisla.delivery.DeliveryZone;
import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneCreateRequest;
import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface DeliveryZoneService {

    List<DeliveryZone> listActive();

    List<DeliveryZone> listAll();

    DeliveryZone getById(UUID zoneId);

    /** Throws {@code DeliveryZoneNotFoundException} if inactive too — checkout must never fall
     *  back to a zone the storefront no longer offers. */
    DeliveryZone getActiveById(UUID zoneId);

    DeliveryZone create(DeliveryZoneCreateRequest request, UUID actingUserId);

    DeliveryZone update(UUID zoneId, DeliveryZoneUpdateRequest request, UUID actingUserId);
}
