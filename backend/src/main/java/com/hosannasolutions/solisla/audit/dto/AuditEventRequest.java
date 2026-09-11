package com.hosannasolutions.solisla.audit.dto;

import com.hosannasolutions.solisla.audit.AuditAction;
import java.util.Map;
import java.util.UUID;

public record AuditEventRequest(
        AuditAction action,
        UUID userId,
        String entityType,
        UUID entityId,
        Map<String, Object> beforeData,
        Map<String, Object> afterData
) {

    public static AuditEventRequest of(AuditAction action, UUID userId) {
        return new AuditEventRequest(action, userId, null, null, null, null);
    }

    public static AuditEventRequest of(AuditAction action, UUID userId, Map<String, Object> beforeData) {
        return new AuditEventRequest(action, userId, null, null, beforeData, null);
    }

    public static AuditEventRequest of(AuditAction action, UUID userId, String entityType, UUID entityId) {
        return new AuditEventRequest(action, userId, entityType, entityId, null, null);
    }
}
