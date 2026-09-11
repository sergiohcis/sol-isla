package com.hosannasolutions.solisla.order.repository;

import com.hosannasolutions.solisla.order.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderIdOrderById(UUID orderId);
}
