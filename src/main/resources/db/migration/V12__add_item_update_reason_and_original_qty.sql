-- =============================================================================
-- Migration V12: Add original_quantity and admin_note to sales_order_items
-- =============================================================================

ALTER TABLE sales_order_items ADD COLUMN original_quantity DECIMAL(14,3);
ALTER TABLE sales_order_items ADD COLUMN admin_note TEXT;

