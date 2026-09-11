/** Stock tracking via an explicit ledger ({@code InventoryMovement}:
 *  PURCHASE/SALE/RESERVATION/RELEASE/ADJUSTMENT/RETURN), not just a counter overwrite. Inventory
 *  changes are transactional with locking/optimistic versioning inside the checkout transaction
 *  to prevent overselling (CLAUDE.md rule 4). */
package com.hosannasolutions.solisla.inventory;
