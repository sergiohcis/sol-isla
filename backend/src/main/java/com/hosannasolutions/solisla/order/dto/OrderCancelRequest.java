package com.hosannasolutions.solisla.order.dto;

import jakarta.validation.constraints.NotBlank;

/** Cancelling a placed order reverses a sale (inventory is restocked) — the reason is required
 *  for the same accountability reasons {@code InventoryAdjustRequest.reason} is. */
public record OrderCancelRequest(@NotBlank String reason) {
}
