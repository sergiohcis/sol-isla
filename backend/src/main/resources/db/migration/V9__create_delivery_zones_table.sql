CREATE TABLE delivery_zones (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    fee        NUMERIC(12, 2) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_delivery_zones_active ON delivery_zones (active);
