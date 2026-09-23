package com.asalkartechworks.daddysodahouse.entity;

/**
 * Invoice classification:
 * - GST: Includes GST calculation at configurable rate
 * - NORMAL: Zero GST calculation
 */
public enum InvoiceType {
    GST,
    NORMAL
}
