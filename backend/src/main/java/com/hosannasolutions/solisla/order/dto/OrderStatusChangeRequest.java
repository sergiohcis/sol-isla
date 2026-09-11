package com.hosannasolutions.solisla.order.dto;

import com.hosannasolutions.solisla.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusChangeRequest(@NotNull OrderStatus status) {
}
