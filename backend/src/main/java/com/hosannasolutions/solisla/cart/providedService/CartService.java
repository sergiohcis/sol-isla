package com.hosannasolutions.solisla.cart.providedService;

import com.hosannasolutions.solisla.cart.dto.CartResponse;
import com.hosannasolutions.solisla.cart.dto.CartSessionResult;
import java.util.UUID;

public interface CartService {

    /** Read-only — an unknown, missing, or expired token returns an empty cart rather than
     *  erroring; no cart is created as a side effect of a GET. */
    CartResponse getCart(String sessionToken);

    CartSessionResult addItem(String sessionToken, UUID productId, int quantity);

    CartSessionResult updateItemQuantity(String sessionToken, UUID itemId, int quantity);

    CartSessionResult removeItem(String sessionToken, UUID itemId);
}
