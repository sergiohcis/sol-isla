package com.hosannasolutions.solisla.checkout.controller;

import com.hosannasolutions.solisla.checkout.dto.CheckoutRequest;
import com.hosannasolutions.solisla.checkout.providedService.CheckoutService;
import com.hosannasolutions.solisla.order.dto.OrderResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private static final String CART_COOKIE_NAME = "sol_isla_cart";

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                   HttpServletRequest httpRequest) {
        return checkoutService.checkout(readCartToken(httpRequest), request, idempotencyKey);
    }

    private String readCartToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CART_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
