package com.hosannasolutions.solisla.audit.repository;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.AuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Extends the bare {@link Repository} marker (not {@code CrudRepository}/{@code JpaRepository})
 * and declares only {@code save}/read methods — no {@code delete} exists on this interface, so
 * audit events cannot be removed by application code, enforcing immutability at compile time.
 */
public interface AuditEventRepository extends Repository<AuditEvent, UUID> {

    AuditEvent save(AuditEvent auditEvent);

    Optional<AuditEvent> findById(UUID id);

    List<AuditEvent> findByUserId(UUID userId);

    List<AuditEvent> findByAction(AuditAction action);
}
