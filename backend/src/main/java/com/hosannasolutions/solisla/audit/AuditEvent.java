package com.hosannasolutions.solisla.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable audit trail row (CLAUDE.md: "security-sensitive operations write to an immutable
 * audit_events log"). No setters, and {@link com.hosannasolutions.solisla.audit.repository.AuditEventRepository}
 * exposes no delete method — normal application code cannot mutate or remove an audit event.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data")
    private Map<String, Object> beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data")
    private Map<String, Object> afterData;

    protected AuditEvent() {
    }

    public AuditEvent(AuditAction action, UUID userId, String entityType, UUID entityId,
                       String ipAddress, String userAgent, Map<String, Object> beforeData, Map<String, Object> afterData) {
        this.action = action;
        this.userId = userId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.occurredAt = Instant.now();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.beforeData = beforeData;
        this.afterData = afterData;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Map<String, Object> getBeforeData() {
        return beforeData;
    }

    public Map<String, Object> getAfterData() {
        return afterData;
    }
}
