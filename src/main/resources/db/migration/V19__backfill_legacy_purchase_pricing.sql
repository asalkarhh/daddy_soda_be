-- =============================================================================
-- Migration V19: Backfill pricing and totals for legacy purchase entries and items
-- Module: Purchase Bills Financials & Supplier Payment Ledger
-- =============================================================================

-- 1. Backfill unit_price and line_total in purchase_items from product default selling price
UPDATE purchase_items pi
SET unit_price = COALESCE(p.default_selling_price, 540.00),
    line_total = pi.quantity * COALESCE(p.default_selling_price, 540.00)
FROM products p
WHERE pi.product_id = p.id
  AND (pi.unit_price IS NULL OR pi.unit_price = 0.00);

-- 2. Backfill subtotal_amount, gst_amount, total_amount, balance_amount, and payment_status in purchase_entries
WITH item_sums AS (
    SELECT purchase_entry_id, COALESCE(SUM(line_total), 0.00) AS calc_subtotal
    FROM purchase_items
    GROUP BY purchase_entry_id
)
UPDATE purchase_entries pe
SET subtotal_amount = s.calc_subtotal,
    gst_rate_percent = 18.00,
    gst_amount = ROUND(s.calc_subtotal * 0.18, 2),
    total_amount = s.calc_subtotal + ROUND(s.calc_subtotal * 0.18, 2),
    balance_amount = (s.calc_subtotal + ROUND(s.calc_subtotal * 0.18, 2)) - COALESCE(pe.paid_amount, 0.00),
    payment_status = CASE
        WHEN COALESCE(pe.paid_amount, 0.00) >= (s.calc_subtotal + ROUND(s.calc_subtotal * 0.18, 2)) AND (s.calc_subtotal + ROUND(s.calc_subtotal * 0.18, 2)) > 0 THEN 'PAID'
        WHEN COALESCE(pe.paid_amount, 0.00) > 0 THEN 'PARTIALLY_PAID'
        ELSE 'UNPAID'
    END
FROM item_sums s
WHERE pe.id = s.purchase_entry_id
  AND (pe.total_amount IS NULL OR pe.total_amount = 0.00);
