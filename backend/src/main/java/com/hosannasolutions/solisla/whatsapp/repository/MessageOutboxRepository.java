package com.hosannasolutions.solisla.whatsapp.repository;

import com.hosannasolutions.solisla.whatsapp.MessageOutbox;
import com.hosannasolutions.solisla.whatsapp.MessageOutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageOutboxRepository extends JpaRepository<MessageOutbox, UUID> {

    /** Rows the worker should attempt now: PENDING and either never attempted
     *  ({@code next_attempt_at IS NULL}, i.e. brand new) or whose backoff has elapsed. */
    @Query("""
            SELECT m FROM MessageOutbox m
            WHERE m.status = :status
              AND (m.nextAttemptAt IS NULL OR m.nextAttemptAt <= :now)
            ORDER BY m.createdAt ASC
            """)
    List<MessageOutbox> findReadyToProcess(@Param("status") MessageOutboxStatus status, @Param("now") Instant now, Pageable limit);
}
