package com.asalkartechworks.daddysodahouse.entity;

/**
 * Payment status lifecycle of an invoice:
 * - UNPAID: Paid amount is zero and due date has not passed
 * - PARTIALLY_PAID: Partial amount collected, balance remains, due date has not passed
 * - PAID: Balance amount is zero
 * - OVERDUE: Balance remains (> 0) and due date is strictly before current date
 */
public enum InvoicePaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}
