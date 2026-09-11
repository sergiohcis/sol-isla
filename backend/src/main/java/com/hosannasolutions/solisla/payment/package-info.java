/** Card and Cash-on-Delivery payment handling. Card payments go through a PCI-compliant,
 *  hosted/tokenized provider — card details never touch this app, and a payment is only marked
 *  {@code PAID} from a provider webhook, never from the browser's success response alone
 *  (CLAUDE.md rule 8). Prefer creating the payment session only after the business has confirmed
 *  the order over WhatsApp. */
package com.hosannasolutions.solisla.payment;
