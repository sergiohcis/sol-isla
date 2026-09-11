package com.hosannasolutions.solisla.checkout.providedService;

import com.hosannasolutions.solisla.checkout.dto.CheckoutRequest;
import com.hosannasolutions.solisla.order.dto.OrderResponse;

public interface CheckoutService {

    /** {@code idempotencyKey} may be null/blank — idempotency protection is opt-in per the
     *  design doc §23 (the client is expected to always send one, but nothing here requires it). */
    OrderResponse checkout(String cartSessionToken, CheckoutRequest request, String idempotencyKey);
}
