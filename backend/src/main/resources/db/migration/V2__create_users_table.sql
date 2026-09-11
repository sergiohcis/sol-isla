CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(32) NOT NULL CHECK (role IN ('ADMIN', 'STAFF')),
    status        VARCHAR(32) NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_users_email ON users (lower(email));
