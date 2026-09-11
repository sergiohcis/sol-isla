package com.hosannasolutions.solisla.order.providedService;

import com.hosannasolutions.solisla.order.Order;
import java.util.UUID;

/** Order lookup only so far (customer tracking by number, the WhatsApp worker by id); Phase 6
 *  extends this with admin listing/status-change methods. */
public interface OrderService {

    Order getByOrderNumber(String orderNumber);

    Order getById(UUID orderId);
}
