CREATE TABLE products (
    id                       UUID PRIMARY KEY,
    sku                      VARCHAR(64) NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    slug                     VARCHAR(255) NOT NULL,
    description              TEXT NULL,
    category_id              UUID NOT NULL REFERENCES categories (id),
    base_price               NUMERIC(12, 2) NOT NULL,
    currency                 VARCHAR(3) NOT NULL,
    discount_type            VARCHAR(16) NOT NULL DEFAULT 'NONE' CHECK (discount_type IN ('NONE', 'PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value           NUMERIC(12, 2) NULL,
    discount_effective_from  TIMESTAMPTZ NULL,
    discount_effective_to    TIMESTAMPTZ NULL,
    status                   VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')),
    featured                 BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL,
    version                  BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_products_sku ON products (sku);
CREATE UNIQUE INDEX ux_products_slug ON products (slug);
CREATE INDEX ix_products_category_id ON products (category_id);
CREATE INDEX ix_products_status ON products (status);
