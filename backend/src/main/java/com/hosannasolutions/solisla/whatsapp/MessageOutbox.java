package com.hosannasolutions.solisla.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Transactional outbox row (design doc §22 / CLAUDE.md rule 6), written in the same transaction as
 * whatever created it — never call the WhatsApp API in that same transaction. {@code payload} is
 * deliberately minimal (just enough to re-load the aggregate) rather than a fully-rendered
 * message: Phase 5's worker renders the actual template from fresh data at send time, so a
 * message queued now and sent minutes later after retries still reflects reality, and Phase 5 is
 * free to redesign the message template without a payload-schema migration.
 */
@Entity
@Table(name = "message_outbox")
public class MessageOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected MessageOutbox() {
    }

    public MessageOutbox(String aggregateType, UUID aggregateId, String messageType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.messageType = messageType;
        this.payload = payload;
        this.status = MessageOutboxStatus.PENDING;
        this.attemptCount = 0;
        this.createdAt = Instant.now();
    }

    public void markProcessing() {
        this.status = MessageOutboxStatus.PROCESSING;
    }

    public void markSent() {
        this.status = MessageOutboxStatus.SENT;
        this.sentAt = Instant.now();
        this.lastError = null;
    }

    /** Records a failed attempt and schedules a retry — status goes back to {@code PENDING}
     *  (not a distinct "retrying" state) since the design doc's four-value status vocabulary
     *  treats PENDING as "will be attempted", including reattempts. */
    public void recordRetryableFailure(String error, Instant nextAttemptAt) {
        this.attemptCount++;
        this.lastError = truncate(error);
        this.nextAttemptAt = nextAttemptAt;
        this.status = MessageOutboxStatus.PENDING;
    }

    /** Terminal — max attempts exhausted. An operator can requeue manually if needed; nothing
     *  does so automatically in Phase 5. */
    public void markPermanentlyFailed(String error) {
        this.attemptCount++;
        this.lastError = truncate(error);
        this.status = MessageOutboxStatus.FAILED;
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getPayload() {
        return payload;
    }

    public MessageOutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
