package com.hosannasolutions.solisla.order.exception;

import com.hosannasolutions.solisla.order.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition order status from " + from + " to " + to);
    }
}
