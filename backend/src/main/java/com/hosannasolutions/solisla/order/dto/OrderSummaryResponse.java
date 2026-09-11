package com.hosannasolutions.solisla.order.dto;

import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.OrderStatus;
import com.hosannasolutions.solisla.order.PaymentMethod;
import com.hosannasolutions.solisla.order.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Admin list row — no items, avoiding an items-join per row (mirrors {@code ProductSummaryResponse}
 *  vs {@code ProductResponse}). */
public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        String customerName,
        String customerPhone,
        BigDecimal grandTotal,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        Instant createdAt
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(), order.getOrderNumber(), order.getCustomerName(), order.getCustomerPhone(),
                order.getGrandTotal(), order.getCurrency(), order.getPaymentMethod(), order.getPaymentStatus(),
                order.getOrderStatus(), order.getCreatedAt());
    }
}
