package com.hosannasolutions.solisla.order.providedService;

import com.hosannasolutions.solisla.order.Order;
import java.util.UUID;

/** Order lookup only so far (customer tracking, the WhatsApp worker by id); Phase 6 extends this
 *  with admin listing/status-change methods. */
public interface OrderService {

    /** Trusted lookup — no ownership check. Internal use only (the WhatsApp worker, future admin
     *  endpoints); never expose this to an unauthenticated caller. */
    Order getByOrderNumber(String orderNumber);

    Order getById(UUID orderId);

    /**
     * Customer-facing lookup: order number alone is not a valid credential (design doc §53 —
     * "do not expose another customer's order merely because someone knows a sequential order
     * number"), so the caller must also supply the phone number that order was placed with. A
     * mismatch throws the same {@code OrderNotFoundException} as a nonexistent order number, so
     * a caller can't distinguish "wrong phone" from "no such order" and enumerate valid numbers.
     */
    Order getByOrderNumberAndPhone(String orderNumber, String phone);
}
