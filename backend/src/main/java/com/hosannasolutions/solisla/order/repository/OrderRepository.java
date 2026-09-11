package com.hosannasolutions.solisla.order.repository;

import com.hosannasolutions.solisla.order.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    /** Design doc §18: monotonic, race-free — a plain counter ("today's order count + 1") would
     *  need locking to be safe under concurrent checkouts; a sequence doesn't. */
    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumberSequence();
}
