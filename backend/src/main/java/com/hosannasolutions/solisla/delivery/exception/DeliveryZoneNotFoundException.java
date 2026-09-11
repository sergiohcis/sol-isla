package com.hosannasolutions.solisla.delivery.exception;

import java.util.UUID;

public class DeliveryZoneNotFoundException extends RuntimeException {

    public DeliveryZoneNotFoundException(UUID zoneId) {
        super("Delivery zone not found: " + zoneId);
    }
}
