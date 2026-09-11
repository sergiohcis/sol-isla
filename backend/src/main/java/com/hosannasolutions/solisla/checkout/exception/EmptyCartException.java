package com.hosannasolutions.solisla.checkout.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Cannot check out an empty cart");
    }
}
