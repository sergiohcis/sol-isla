package com.hosannasolutions.solisla.inventory.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

/** {@code delta} may be positive (restock) or negative (shrinkage/correction) but not zero — a
 *  no-op adjustment isn't a real ledger entry. Also re-checked in {@code InventoryServiceImpl}
 *  as a backstop for any future non-HTTP caller. */
public record InventoryAdjustRequest(int delta, @NotBlank String reason) {

    @AssertTrue(message = "delta must not be zero")
    public boolean isDeltaNonZero() {
        return delta != 0;
    }
}
