package com.hosannasolutions.solisla.checkout.dto;

import com.hosannasolutions.solisla.order.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckoutRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @Email String customerEmail,
        @NotBlank String deliveryAddress,
        @NotBlank String deliveryCity,
        String deliveryPostalCode,
        String deliveryNotes,
        @NotNull UUID deliveryZoneId,
        @NotNull PaymentMethod paymentMethod
) {
}
