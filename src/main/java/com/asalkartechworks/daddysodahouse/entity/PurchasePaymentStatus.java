package com.asalkartechworks.daddysodahouse.entity;

/**
 * Payment status for supplier purchase bills:
 * - UNPAID: No payment has been made yet to the supplier.
 * - PARTIALLY_PAID: Partial payment recorded, balance remains pending.
 * - PAID: Full payment made to supplier, pending balance is zero.
 */
public enum PurchasePaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID
}
