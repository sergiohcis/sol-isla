package com.hosannasolutions.solisla.order.providedService;

import com.hosannasolutions.solisla.common.api.PageResponse;
import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.OrderStatus;
import com.hosannasolutions.solisla.order.dto.OrderSummaryResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Order lookup, admin listing, and the Phase 6 admin status-change/cancel operations. */
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

    /** Admin listing (design doc §11-style filters, mirroring {@code ProductService.searchAdmin}):
     *  {@code status = null} means "all statuses", {@code q} matches order number, customer name,
     *  or phone. */
    PageResponse<OrderSummaryResponse> searchAdmin(OrderStatus status, String q, Pageable pageable);

    /**
     * Forward/branch transitions a staff member routinely makes while reviewing and fulfilling
     * orders (confirm, reject, move through preparation/delivery, mark a delivery failed/returned).
     * Validated against a fixed transition table, mirroring {@code ProductService.changeStatus}.
     * Rejecting or returning an order also restocks the inventory that checkout already sold, in
     * the same transaction.
     */
    Order changeStatus(UUID orderId, OrderStatus newStatus, UUID actingUserId);

    /**
     * The one transition that requires {@code ORDER_CANCEL} rather than {@code ORDER_UPDATE_STATUS}
     * (see {@code RolePermissions} — ADMIN-only by default): undoing an order after it was placed,
     * restocking inventory in the same transaction. Allowed from any status except the terminal
     * ones and {@code DELIVERED}.
     */
    Order cancel(UUID orderId, String reason, UUID actingUserId);
}
