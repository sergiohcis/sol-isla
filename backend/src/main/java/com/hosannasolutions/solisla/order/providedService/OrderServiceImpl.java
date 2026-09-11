package com.hosannasolutions.solisla.order.providedService;

import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.exception.OrderNotFoundException;
import com.hosannasolutions.solisla.order.repository.OrderRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order getByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    @Override
    public Order getById(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
