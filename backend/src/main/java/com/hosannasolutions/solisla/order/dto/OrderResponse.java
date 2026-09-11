package com.hosannasolutions.solisla.order.dto;

import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.OrderStatus;
import com.hosannasolutions.solisla.order.PaymentMethod;
import com.hosannasolutions.solisla.order.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryCity,
        String deliveryPostalCode,
        String deliveryNotes,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal deliveryFee,
        BigDecimal grandTotal,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        Instant createdAt
) {

    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(), order.getOrderNumber(), order.getCustomerName(), order.getCustomerPhone(), order.getCustomerEmail(),
                order.getDeliveryAddress(), order.getDeliveryCity(), order.getDeliveryPostalCode(), order.getDeliveryNotes(),
                items, order.getSubtotal(), order.getDiscountTotal(), order.getDeliveryFee(), order.getGrandTotal(),
                order.getCurrency(), order.getPaymentMethod(), order.getPaymentStatus(), order.getOrderStatus(),
                order.getCreatedAt());
    }
}
