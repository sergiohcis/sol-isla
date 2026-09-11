package com.hosannasolutions.solisla.whatsapp.repository;

import com.hosannasolutions.solisla.whatsapp.MessageOutbox;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Just persistence for now — Phase 5 adds the status-polling queries its background worker
 *  needs. */
public interface MessageOutboxRepository extends JpaRepository<MessageOutbox, UUID> {
}
