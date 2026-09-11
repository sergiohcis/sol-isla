package com.hosannasolutions.solisla.cart.dto;

/** {@code sessionToken} is always the current, valid token for the cart just mutated — the
 *  controller re-sets the cookie with it on every mutation (cheap, and keeps a sliding
 *  expiration; see {@code Cart.touch()}). */
public record CartSessionResult(String sessionToken, CartResponse cart) {
}
