package com.asalkartechworks.daddysodahouse.entity;

/**
 * Payment terms for an invoice:
 * - FULL_PAYMENT: No due date required
 * - PARTIAL_PAYMENT: Due date applies for the pending balance
 * - CREDIT: Due date applies (defaults to invoice_date + 10 days)
 */
public enum PaymentTerm {
    FULL_PAYMENT,
    PARTIAL_PAYMENT,
    CREDIT
}
