-- =============================================================================
-- Migration V18: Add pricing, GST calculations, and payment tracking to purchases
-- Module: Purchase Bills Financials & Supplier Payment Ledger
-- =============================================================================

-- 1. Add financial columns to purchase_entries
ALTER TABLE purchase_entries
    ADD COLUMN IF NOT EXISTS subtotal_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS gst_rate_percent DECIMAL(5,2) NOT NULL DEFAULT 18.00,
    ADD COLUMN IF NOT EXISTS gst_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS total_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS balance_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID';

CREATE INDEX IF NOT EXISTS idx_purchase_entries_payment_status ON purchase_entries(payment_status);

-- 2. Add pricing columns to purchase_items
ALTER TABLE purchase_items
    ADD COLUMN IF NOT EXISTS unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS line_total DECIMAL(14,2) NOT NULL DEFAULT 0.00;

-- 3. Create purchase_payments table for supplier payment ledger
CREATE TABLE IF NOT EXISTS purchase_payments (
    id UUID PRIMARY KEY,
    purchase_entry_id UUID NOT NULL REFERENCES purchase_entries(id) ON DELETE CASCADE,
    payment_date DATE NOT NULL,
    amount DECIMAL(14,2) NOT NULL CHECK (amount > 0),
    payment_mode VARCHAR(30) NOT NULL CHECK (payment_mode IN ('CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'OTHER')),
    reference_number VARCHAR(100),
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_purchase_payments_entry_id ON purchase_payments(purchase_entry_id);
CREATE INDEX IF NOT EXISTS idx_purchase_payments_payment_date ON purchase_payments(payment_date);
