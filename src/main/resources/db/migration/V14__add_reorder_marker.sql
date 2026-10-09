ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS reordered_from_order_number VARCHAR(40);
