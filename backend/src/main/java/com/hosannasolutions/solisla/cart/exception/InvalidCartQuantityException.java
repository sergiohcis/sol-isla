package com.hosannasolutions.solisla.cart.exception;

/** Covers both "over the configured per-item maximum" and "more than currently in stock" —
 *  both are the same client-facing situation (requested quantity isn't purchasable right now). */
public class InvalidCartQuantityException extends RuntimeException {

    public InvalidCartQuantityException(String message) {
        super(message);
    }
}
