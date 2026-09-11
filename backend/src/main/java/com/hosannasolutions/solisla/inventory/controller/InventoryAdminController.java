package com.hosannasolutions.solisla.inventory.controller;

import com.hosannasolutions.solisla.inventory.dto.InventoryAdjustRequest;
import com.hosannasolutions.solisla.inventory.dto.InventoryResponse;
import com.hosannasolutions.solisla.inventory.providedService.InventoryService;
import com.hosannasolutions.solisla.security.userdetails.SolIslaUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryAdminController {

    private final InventoryService inventoryService;

    public InventoryAdminController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public InventoryResponse get(@PathVariable UUID productId) {
        return InventoryResponse.from(inventoryService.getByProductId(productId));
    }

    @PostMapping("/{productId}/adjust")
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public InventoryResponse adjust(@PathVariable UUID productId, @Valid @RequestBody InventoryAdjustRequest request,
                                     @AuthenticationPrincipal SolIslaUserPrincipal principal) {
        return InventoryResponse.from(inventoryService.adjust(productId, request.delta(), request.reason(), principal.getUserId()));
    }
}
