package com.hosannasolutions.solisla.order.providedService;

import com.hosannasolutions.solisla.order.Order;

/** Phase 4 only needs order lookup (customer tracking); Phase 6 extends this with admin
 *  listing/status-change methods. */
public interface OrderService {

    Order getByOrderNumber(String orderNumber);
}
