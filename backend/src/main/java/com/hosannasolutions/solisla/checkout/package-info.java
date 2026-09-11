/** {@code CheckoutService} is the most important service in this codebase: it orchestrates
 *  idempotency check → load cart → server-side pricing recalculation → inventory reservation →
 *  order creation → payment initialization → outbox enqueue, all inside one
 *  {@code @Transactional} boundary (external HTTP calls excluded from that transaction). Requires
 *  a client-supplied {@code Idempotency-Key} to survive duplicate submissions (CLAUDE.md rule 7). */
package com.hosannasolutions.solisla.checkout;
