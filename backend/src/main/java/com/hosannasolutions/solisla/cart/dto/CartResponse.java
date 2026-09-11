package com.hosannasolutions.solisla.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        List<CartItemResponse> items,
        int totalQuantity,
        BigDecimal subtotal,
        String currency
) {

    public static CartResponse empty() {
        return new CartResponse(null, List.of(), 0, BigDecimal.ZERO, null);
    }
}
