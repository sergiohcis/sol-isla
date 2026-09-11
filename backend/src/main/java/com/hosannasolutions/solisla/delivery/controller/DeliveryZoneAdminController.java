package com.hosannasolutions.solisla.delivery.controller;

import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneCreateRequest;
import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneResponse;
import com.hosannasolutions.solisla.delivery.dto.DeliveryZoneUpdateRequest;
import com.hosannasolutions.solisla.delivery.providedService.DeliveryZoneService;
import com.hosannasolutions.solisla.security.userdetails.SolIslaUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/delivery/zones")
@PreAuthorize("hasAuthority('DELIVERY_MANAGE')")
public class DeliveryZoneAdminController {

    private final DeliveryZoneService deliveryZoneService;

    public DeliveryZoneAdminController(DeliveryZoneService deliveryZoneService) {
        this.deliveryZoneService = deliveryZoneService;
    }

    @GetMapping
    public List<DeliveryZoneResponse> list() {
        return deliveryZoneService.listAll().stream().map(DeliveryZoneResponse::from).toList();
    }

    @PostMapping
    public DeliveryZoneResponse create(@Valid @RequestBody DeliveryZoneCreateRequest request,
                                        @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return DeliveryZoneResponse.from(deliveryZoneService.create(request, principal.getUserId()));
    }

    @PutMapping("/{zoneId}")
    public DeliveryZoneResponse update(@PathVariable UUID zoneId, @Valid @RequestBody DeliveryZoneUpdateRequest request,
                                        @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return DeliveryZoneResponse.from(deliveryZoneService.update(zoneId, request, principal.getUserId()));
    }
}
