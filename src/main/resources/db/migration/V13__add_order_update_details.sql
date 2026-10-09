ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS update_reason TEXT;

ALTER TABLE sales_order_items
    ADD COLUMN IF NOT EXISTS original_quantity NUMERIC(14, 3),
    ADD COLUMN IF NOT EXISTS admin_note TEXT;
