package com.hosannasolutions.solisla.whatsapp.providedService;

import com.hosannasolutions.solisla.audit.AuditAction;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.providedService.AuditService;
import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.order.providedService.OrderService;
import com.hosannasolutions.solisla.whatsapp.MessageOutbox;
import com.hosannasolutions.solisla.whatsapp.MessageOutboxStatus;
import com.hosannasolutions.solisla.whatsapp.dto.WhatsAppSendResult;
import com.hosannasolutions.solisla.whatsapp.repository.MessageOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Retry policy is deliberately hardcoded, not configuration (design doc §25's example config
 *  keys don't include one, and a single-store MVP doesn't need it tunable yet). */
@Service
public class OutboxProcessingServiceImpl implements OutboxProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessingServiceImpl.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long MAX_BACKOFF_MINUTES = 60;
    private static final String MESSAGE_TYPE_ORDER_CREATED = "ORDER_CREATED";

    private final MessageOutboxRepository messageOutboxRepository;
    private final OrderService orderService;
    private final WhatsAppService whatsAppService;
    private final AuditService auditService;

    public OutboxProcessingServiceImpl(MessageOutboxRepository messageOutboxRepository, OrderService orderService,
                                        WhatsAppService whatsAppService, AuditService auditService) {
        this.messageOutboxRepository = messageOutboxRepository;
        this.orderService = orderService;
        this.whatsAppService = whatsAppService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void processOne(UUID outboxId) {
        MessageOutbox outbox = messageOutboxRepository.findById(outboxId).orElse(null);
        if (outbox == null || outbox.getStatus() != MessageOutboxStatus.PENDING) {
            return; // already handled by a previous run, or gone — nothing to do
        }
        outbox.markProcessing();

        WhatsAppSendResult result = switch (outbox.getMessageType()) {
            case MESSAGE_TYPE_ORDER_CREATED -> sendOrderCreated(outbox);
            default -> WhatsAppSendResult.failure("Unknown message type: " + outbox.getMessageType());
        };

        if (result.success()) {
            outbox.markSent();
            auditService.record(AuditEventRequest.of(
                    AuditAction.WHATSAPP_MESSAGE_SENT, null, outbox.getAggregateType(), outbox.getAggregateId()));
        } else {
            recordFailure(outbox, result.errorMessage());
        }
        messageOutboxRepository.save(outbox);
    }

    private WhatsAppSendResult sendOrderCreated(MessageOutbox outbox) {
        Order order;
        try {
            order = orderService.getById(outbox.getAggregateId());
        } catch (RuntimeException e) {
            return WhatsAppSendResult.failure("Order not found: " + outbox.getAggregateId());
        }
        return whatsAppService.sendOrderCreatedMessage(order);
    }

    private void recordFailure(MessageOutbox outbox, String errorMessage) {
        int nextAttemptNumber = outbox.getAttemptCount() + 1;
        if (nextAttemptNumber >= MAX_ATTEMPTS) {
            outbox.markPermanentlyFailed(errorMessage);
            log.warn("WhatsApp send permanently failed for outbox {} after {} attempts: {}",
                    outbox.getId(), nextAttemptNumber, errorMessage);
        } else {
            long backoffMinutes = Math.min(MAX_BACKOFF_MINUTES, 1L << nextAttemptNumber);
            outbox.recordRetryableFailure(errorMessage, Instant.now().plus(Duration.ofMinutes(backoffMinutes)));
            log.info("WhatsApp send attempt {} failed for outbox {}, retrying in {} min: {}",
                    nextAttemptNumber, outbox.getId(), backoffMinutes, errorMessage);
        }
        auditService.record(AuditEventRequest.of(
                AuditAction.WHATSAPP_MESSAGE_FAILED, null, outbox.getAggregateType(), outbox.getAggregateId()));
    }
}
