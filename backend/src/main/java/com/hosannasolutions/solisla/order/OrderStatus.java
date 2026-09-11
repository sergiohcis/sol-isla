package com.hosannasolutions.solisla.order;

/** Design doc §17 — full lifecycle. Transition validation (a defined table, mirroring
 *  {@code ProductStatus}'s ALLOWED_TRANSITIONS) is added in Phase 6 alongside the admin endpoints
 *  that actually move an order between these states; Phase 4 only ever creates an order in
 *  {@code PENDING_CONFIRMATION}. */
public enum OrderStatus {
    PENDING_CONFIRMATION,
    CONFIRMED,
    PREPARING,
    READY_FOR_DELIVERY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REJECTED,
    FAILED_DELIVERY,
    RETURNED
}
