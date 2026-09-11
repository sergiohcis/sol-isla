package com.hosannasolutions.solisla.whatsapp.providedService;

import java.util.UUID;

/** One outbox row per call, each in its own transaction — kept separate from {@code OutboxWorker}
 *  (a plain, non-transactional {@code @Component}) purely so {@code @Transactional} takes effect:
 *  a method calling another {@code @Transactional} method on {@code this} bypasses the Spring AOP
 *  proxy entirely. */
public interface OutboxProcessingService {

    void processOne(UUID outboxId);
}
