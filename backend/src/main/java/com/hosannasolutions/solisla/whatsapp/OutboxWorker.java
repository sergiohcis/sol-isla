package com.hosannasolutions.solisla.whatsapp;

import com.hosannasolutions.solisla.whatsapp.providedService.OutboxProcessingService;
import com.hosannasolutions.solisla.whatsapp.providedService.WhatsAppService;
import com.hosannasolutions.solisla.whatsapp.repository.MessageOutboxRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background worker for the transactional outbox (design doc §22) — the only thing in this
 * codebase that calls the WhatsApp API, and it never does so inside the transaction that created
 * the outbox row (CLAUDE.md rule 6). Polls rather than reacting to an event: simple, and correct
 * even after an app restart with rows still PENDING from before.
 */
@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final int BATCH_SIZE = 20;

    private final MessageOutboxRepository messageOutboxRepository;
    private final OutboxProcessingService outboxProcessingService;
    private final WhatsAppService whatsAppService;

    public OutboxWorker(MessageOutboxRepository messageOutboxRepository, OutboxProcessingService outboxProcessingService,
                         WhatsAppService whatsAppService) {
        this.messageOutboxRepository = messageOutboxRepository;
        this.outboxProcessingService = outboxProcessingService;
        this.whatsAppService = whatsAppService;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 30_000)
    public void processPendingMessages() {
        if (!whatsAppService.isConfigured()) {
            return; // rows just stay PENDING until an operator configures sol-isla.whatsapp.*
        }
        List<MessageOutbox> batch = messageOutboxRepository.findReadyToProcess(
                MessageOutboxStatus.PENDING, Instant.now(), PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) {
            return;
        }
        log.debug("Processing {} pending WhatsApp outbox message(s)", batch.size());
        for (MessageOutbox message : batch) {
            outboxProcessingService.processOne(message.getId());
        }
    }
}
