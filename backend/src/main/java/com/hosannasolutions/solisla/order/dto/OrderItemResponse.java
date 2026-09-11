package com.hosannasolutions.solisla.order.dto;

import com.hosannasolutions.solisla.order.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
        String skuSnapshot,
        String productNameSnapshot,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal finalUnitPrice,
        int quantity,
        BigDecimal lineTotal
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getSkuSnapshot(), item.getProductNameSnapshot(), item.getUnitPrice(), item.getDiscountAmount(),
                item.getFinalUnitPrice(), item.getQuantity(), item.getLineTotal());
    }
}
