/** WhatsApp is a communication/confirmation channel only — PostgreSQL is always the sole source
 *  of truth for the order record (CLAUDE.md rule 1). Delivery uses the transactional outbox
 *  pattern: a {@code MessageOutbox} row is written in the same transaction as order creation,
 *  then sent asynchronously by a background worker with retry/backoff — the WhatsApp API is
 *  never called inside that transaction, so a WhatsApp outage can never roll back or hide a
 *  successfully created order (rule 6). */
package com.hosannasolutions.solisla.whatsapp;
