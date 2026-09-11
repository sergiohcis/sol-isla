package com.hosannasolutions.solisla.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Identified by an unguessable random {@code sessionToken} carried in an httpOnly cookie — not by
 * a logged-in user, since guest checkout is the MVP default and there are no customer accounts
 * (CLAUDE.md rule 11). {@code currency} is {@code null} until the first item is added, then fixed
 * to that item's product currency; every later add must match it (a single-store deployment isn't
 * expected to mix currencies within one cart).
 */
@Entity
@Table(name = "carts")
public class Cart {

    /** Also the cart cookie's max-age (see CartController) — one source of truth for "how long a
     *  guest cart survives without activity". */
    public static final Duration LIFETIME = Duration.ofDays(30);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_token", nullable = false)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected Cart() {
    }

    public static Cart createNew() {
        Cart cart = new Cart();
        Instant now = Instant.now();
        cart.sessionToken = UUID.randomUUID().toString();
        cart.status = CartStatus.ACTIVE;
        cart.createdAt = now;
        cart.updatedAt = now;
        cart.expiresAt = now.plus(LIFETIME);
        return cart;
    }

    /** Called on every mutation — keeps an actively-shopped cart from expiring mid-session. */
    public void touch() {
        this.updatedAt = Instant.now();
        this.expiresAt = this.updatedAt.plus(LIFETIME);
    }

    public void assignCurrencyIfUnset(String productCurrency) {
        if (this.currency == null) {
            this.currency = productCurrency;
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public CartStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
