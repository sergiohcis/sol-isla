-- Design doc §18: a human-friendly order number ("ORD-20260910-000421") distinct from the
-- internal UUID. A plain Postgres sequence avoids the race conditions a "count today's orders + 1"
-- approach would have under concurrent checkouts.
CREATE SEQUENCE order_number_seq START WITH 1;

CREATE TABLE orders (
    id                   UUID PRIMARY KEY,
    order_number         VARCHAR(32) NOT NULL,
    -- Postgres unique indexes allow multiple NULLs, so requests with no Idempotency-Key header
    -- (idempotency is opt-in per design doc §23) never collide with each other.
    idempotency_key      VARCHAR(255) NULL,
    customer_name        VARCHAR(255) NOT NULL,
    customer_phone       VARCHAR(32) NOT NULL,
    customer_email       VARCHAR(255) NULL,
    delivery_address     VARCHAR(500) NOT NULL,
    delivery_city        VARCHAR(255) NOT NULL,
    delivery_postal_code VARCHAR(32) NULL,
    delivery_notes       VARCHAR(1000) NULL,
    delivery_zone_id     UUID NOT NULL REFERENCES delivery_zones (id),
    subtotal             NUMERIC(12, 2) NOT NULL,
    discount_total       NUMERIC(12, 2) NOT NULL,
    delivery_fee         NUMERIC(12, 2) NOT NULL,
    grand_total          NUMERIC(12, 2) NOT NULL,
    currency             VARCHAR(3) NOT NULL,
    payment_method       VARCHAR(32) NOT NULL CHECK (payment_method IN ('CARD', 'CASH_ON_DELIVERY')),
    payment_status       VARCHAR(16) NOT NULL CHECK (payment_status IN ('NOT_REQUIRED', 'PENDING', 'AUTHORIZED', 'PAID', 'FAILED', 'CANCELLED', 'REFUNDED')),
    order_status         VARCHAR(24) NOT NULL CHECK (order_status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'PREPARING', 'READY_FOR_DELIVERY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'REJECTED', 'FAILED_DELIVERY', 'RETURNED')),
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_orders_order_number ON orders (order_number);
CREATE UNIQUE INDEX ux_orders_idempotency_key ON orders (idempotency_key);
CREATE INDEX ix_orders_delivery_zone_id ON orders (delivery_zone_id);

-- Design doc §9 / CLAUDE.md rule 3: immutable price/product snapshot, never recomputed from the
-- current product — a later price or name change must never alter historical orders.
CREATE TABLE order_items (
    id                     UUID PRIMARY KEY,
    order_id               UUID NOT NULL REFERENCES orders (id),
    product_id             UUID NOT NULL REFERENCES products (id),
    sku_snapshot           VARCHAR(64) NOT NULL,
    product_name_snapshot  VARCHAR(255) NOT NULL,
    unit_price             NUMERIC(12, 2) NOT NULL,
    discount_amount        NUMERIC(12, 2) NOT NULL,
    final_unit_price       NUMERIC(12, 2) NOT NULL,
    quantity               INTEGER NOT NULL,
    line_total             NUMERIC(12, 2) NOT NULL
);

CREATE INDEX ix_order_items_order_id ON order_items (order_id);
