package com.hosannasolutions.solisla.cart;

public enum CartStatus {
    ACTIVE,
    /** Set when checkout (Phase 4) turns this cart into an order — an immutable historical
     *  marker, not something a cart transitions back out of. */
    CONVERTED
}
