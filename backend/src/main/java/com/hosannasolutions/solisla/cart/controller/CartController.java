package com.hosannasolutions.solisla.cart.controller;

import com.hosannasolutions.solisla.cart.Cart;
import com.hosannasolutions.solisla.cart.dto.AddCartItemRequest;
import com.hosannasolutions.solisla.cart.dto.CartResponse;
import com.hosannasolutions.solisla.cart.dto.CartSessionResult;
import com.hosannasolutions.solisla.cart.dto.UpdateCartItemQuantityRequest;
import com.hosannasolutions.solisla.cart.providedService.CartService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guest cart, identified by an httpOnly cookie carrying a random session token — no accounts, no
 * Spring Security session (CLAUDE.md rule 11). Every mutating endpoint re-sets the cookie with the
 * cart's current token so an already-valid cart's expiry keeps sliding forward ({@link
 * Cart#touch()}); {@link #get} never writes a cookie since it has no side effects to persist.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final String COOKIE_NAME = "sol_isla_cart";

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(HttpServletRequest request) {
        return cartService.getCart(readToken(request));
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request, HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        CartSessionResult result = cartService.addItem(readToken(httpRequest), request.productId(), request.quantity());
        writeToken(httpRequest, httpResponse, result.sessionToken());
        return result.cart();
    }

    @PutMapping("/items/{itemId}")
    public CartResponse updateItemQuantity(@PathVariable UUID itemId, @Valid @RequestBody UpdateCartItemQuantityRequest request,
                                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CartSessionResult result = cartService.updateItemQuantity(readToken(httpRequest), itemId, request.quantity());
        writeToken(httpRequest, httpResponse, result.sessionToken());
        return result.cart();
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@PathVariable UUID itemId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        CartSessionResult result = cartService.removeItem(readToken(httpRequest), itemId);
        writeToken(httpRequest, httpResponse, result.sessionToken());
        return result.cart();
    }

    private String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeToken(HttpServletRequest request, HttpServletResponse response, String sessionToken) {
        Cookie cookie = new Cookie(COOKIE_NAME, sessionToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge((int) Cart.LIFETIME.toSeconds());
        response.addCookie(cookie);
    }
}
