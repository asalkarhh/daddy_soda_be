ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS cancelled_by_role VARCHAR(30);
