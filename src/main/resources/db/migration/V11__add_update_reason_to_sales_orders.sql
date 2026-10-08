-- =============================================================================
-- Migration V11: Add update_reason column to sales_orders table
-- =============================================================================

ALTER TABLE sales_orders ADD COLUMN update_reason TEXT;

