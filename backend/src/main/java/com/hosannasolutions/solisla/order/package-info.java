/** Orders with immutable price/product snapshots ({@code OrderItem.sku_snapshot},
 *  {@code product_name_snapshot}, {@code unit_price}, {@code final_unit_price}, ...) so historical
 *  orders are unaffected by later product/price changes (CLAUDE.md rule 3). Order status and
 *  payment status are separate state machines enforcing a defined transition table (rule 5):
 *  {@code PENDING_CONFIRMATION → CONFIRMED → PREPARING → READY_FOR_DELIVERY → OUT_FOR_DELIVERY →
 *  DELIVERED}, with {@code CANCELLED}/{@code REJECTED}/{@code FAILED_DELIVERY} branches. */
package com.hosannasolutions.solisla.order;
