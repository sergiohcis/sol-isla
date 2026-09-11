CREATE TABLE product_images (
    id          UUID PRIMARY KEY,
    product_id  UUID NOT NULL REFERENCES products (id),
    url         VARCHAR(512) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    alt_text    VARCHAR(255) NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_product_images_product_id ON product_images (product_id);
