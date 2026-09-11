-- Transactional outbox (design doc §22 / CLAUDE.md rule 6): written in the same transaction as
-- order creation. Nothing reads or sends these yet — Phase 5 adds the WhatsApp background worker
-- that polls status='PENDING', renders the actual message from the order, and updates this row.
CREATE TABLE message_outbox (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID NOT NULL,
    message_type    VARCHAR(64) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NULL,
    last_error      VARCHAR(1000) NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    sent_at         TIMESTAMPTZ NULL
);

CREATE INDEX ix_message_outbox_status ON message_outbox (status);
CREATE INDEX ix_message_outbox_aggregate ON message_outbox (aggregate_type, aggregate_id);
