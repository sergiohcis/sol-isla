package com.hosannasolutions.solisla.order.dto;

import jakarta.validation.constraints.NotBlank;

/** POST body rather than query params so the phone number — PII — never lands in a URL, browser
 *  history, or server access log (design doc §53's "secure mechanism", CLAUDE.md's data-handling
 *  posture generally). */
public record OrderTrackingRequest(@NotBlank String orderNumber, @NotBlank String phone) {
}
