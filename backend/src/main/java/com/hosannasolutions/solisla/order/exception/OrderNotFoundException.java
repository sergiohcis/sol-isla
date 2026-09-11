package com.hosannasolutions.solisla.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderNumber) {
        super("Order not found: " + orderNumber);
    }
}
