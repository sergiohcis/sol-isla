package com.hosannasolutions.solisla.cart.repository;

import com.hosannasolutions.solisla.cart.Cart;
import com.hosannasolutions.solisla.cart.CartStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findBySessionTokenAndStatus(String sessionToken, CartStatus status);
}
