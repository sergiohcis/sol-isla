CREATE TABLE carts (
    id            UUID PRIMARY KEY,
    session_token VARCHAR(64) NOT NULL,
    status        VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'CONVERTED')),
    currency      VARCHAR(3) NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_carts_session_token ON carts (session_token);

-- Design doc §12: the cart never persists the authoritative price — only product_id and
-- quantity. Price/discount are recalculated live from the current product on every read, and
-- again (transactionally) at checkout.
CREATE TABLE cart_items (
    id         UUID PRIMARY KEY,
    cart_id    UUID NOT NULL REFERENCES carts (id),
    product_id UUID NOT NULL REFERENCES products (id),
    quantity   INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_cart_items_cart_product ON cart_items (cart_id, product_id);
CREATE INDEX ix_cart_items_cart_id ON cart_items (cart_id);
