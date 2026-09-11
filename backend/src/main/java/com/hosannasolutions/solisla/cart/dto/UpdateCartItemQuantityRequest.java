package com.hosannasolutions.solisla.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemQuantityRequest(@Min(1) int quantity) {
}
