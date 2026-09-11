package com.hosannasolutions.solisla.delivery.controller;

import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneResponse;
import com.hosannasolutions.solisla.delivery.providedService.DeliveryZoneService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, active-only zones — lets the checkout form show delivery cost before submission
 *  (design doc §24). See {@code DeliveryZoneAdminController} for the authenticated CRUD surface. */
@RestController
@RequestMapping("/api/delivery/zones")
public class DeliveryZoneController {

    private final DeliveryZoneService deliveryZoneService;

    public DeliveryZoneController(DeliveryZoneService deliveryZoneService) {
        this.deliveryZoneService = deliveryZoneService;
    }

    @GetMapping
    public List<DeliveryZoneResponse> list() {
        return deliveryZoneService.listActive().stream().map(DeliveryZoneResponse::from).toList();
    }
}
