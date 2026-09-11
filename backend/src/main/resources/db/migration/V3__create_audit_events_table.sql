CREATE TABLE audit_events (
    id          UUID PRIMARY KEY,
    user_id     UUID NULL,
    action      VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NULL,
    entity_id   UUID NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    ip_address  VARCHAR(64) NULL,
    user_agent  VARCHAR(512) NULL,
    before_data JSONB NULL,
    after_data  JSONB NULL
);

CREATE INDEX ix_audit_events_user_id ON audit_events (user_id);
CREATE INDEX ix_audit_events_occurred_at ON audit_events (occurred_at);
