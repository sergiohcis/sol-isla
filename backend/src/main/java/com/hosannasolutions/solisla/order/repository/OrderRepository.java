package com.hosannasolutions.solisla.order.repository;

import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    /** Design doc §18: monotonic, race-free — a plain counter ("today's order count + 1") would
     *  need locking to be safe under concurrent checkouts; a sequence doesn't. */
    @Query(value = "SELECT nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumberSequence();

    /** Admin listing — mirrors {@code ProductRepository.search} exactly, including the
     *  {@code :q = ''} (never null) requirement: an untyped NULL bind used only inside
     *  {@code lower(...)} defaults to bytea in Postgres and breaks every no-search-term query. */
    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.orderStatus = :status)
              AND (:q = '' OR lower(o.orderNumber) LIKE lower(concat('%', :q, '%'))
                           OR lower(o.customerName) LIKE lower(concat('%', :q, '%'))
                           OR lower(o.customerPhone) LIKE lower(concat('%', :q, '%')))
            """)
    Page<Order> search(@Param("status") OrderStatus status, @Param("q") String q, Pageable pageable);
}
