package com.hosannasolutions.solisla.order;

/** Design doc §14. Both payment methods start {@code PENDING} at order creation — real
 *  {@code CARD} payment session/webhook handling is Phase 7 (CLAUDE.md rule 8: only a provider
 *  webhook may set {@code PAID}, never the browser's success response). */
public enum PaymentStatus {
    NOT_REQUIRED,
    PENDING,
    AUTHORIZED,
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED
}
