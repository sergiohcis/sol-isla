package com.hosannasolutions.solisla.whatsapp.providedService;

import com.hosannasolutions.solisla.order.Order;
import com.hosannasolutions.solisla.whatsapp.dto.WhatsAppSendResult;

/** Design doc §20 — a dedicated integration module so WhatsApp HTTP calls never scatter into
 *  order code. */
public interface WhatsAppService {

    WhatsAppSendResult sendOrderCreatedMessage(Order order);

    /** {@code sol-isla.whatsapp.*} is intentionally empty by default so the app boots without
     *  WhatsApp configured (see application.yml) — {@code OutboxWorker} checks this before ever
     *  querying the outbox, so an unconfigured instance just leaves rows PENDING indefinitely
     *  instead of burning through retries against credentials that will never work. */
    boolean isConfigured();
}
