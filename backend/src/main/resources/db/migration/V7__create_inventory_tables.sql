CREATE TABLE inventory (
    id                  UUID PRIMARY KEY,
    product_id          UUID NOT NULL REFERENCES products (id),
    available_quantity  INTEGER NOT NULL DEFAULT 0,
    reserved_quantity   INTEGER NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL,
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_inventory_product_id ON inventory (product_id);

-- Ledger, not just a counter (design doc §36 / CLAUDE.md rule 4): every stock change is recorded
-- here, the running available_quantity on `inventory` is a derived cache kept in sync in the same
-- transaction.
CREATE TABLE inventory_movements (
    id             UUID PRIMARY KEY,
    product_id     UUID NOT NULL REFERENCES products (id),
    type           VARCHAR(16) NOT NULL CHECK (type IN ('PURCHASE', 'SALE', 'RESERVATION', 'RELEASE', 'ADJUSTMENT', 'RETURN')),
    quantity       INTEGER NOT NULL,
    reference_type VARCHAR(64) NULL,
    reference_id   UUID NULL,
    reason         VARCHAR(255) NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    created_by     UUID NULL REFERENCES users (id)
);

CREATE INDEX ix_inventory_movements_product_id ON inventory_movements (product_id);
