CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    parent_id   UUID NULL REFERENCES categories (id),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_categories_slug ON categories (slug);
CREATE INDEX ix_categories_parent_id ON categories (parent_id);
