package com.hosannasolutions.solisla.cart.exception;

import java.util.UUID;

/** Product exists but isn't ACTIVE (DRAFT/INACTIVE/ARCHIVED) — not addable to a cart. A soft,
 *  friendly-UX check; checkout (Phase 4) re-validates for real inside its transaction. */
public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException(UUID productId) {
        super("Product is not available for purchase: " + productId);
    }
}
