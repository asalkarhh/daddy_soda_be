-- ==============================================================================
-- Daddy Soda House - Migration V10: Manual Daily Flavour Sales Fallback
-- ==============================================================================

CREATE TABLE manual_daily_flavour_sales (
    id UUID PRIMARY KEY,
    franchise_id UUID NOT NULL REFERENCES franchises(id),
    machine_id UUID NOT NULL REFERENCES machines(id),
    product_id UUID NOT NULL REFERENCES products(id),
    sales_date DATE NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    quantity_unit VARCHAR(20) NOT NULL DEFAULT 'GLASSES',
    notes TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    CONSTRAINT uq_manual_sales_machine_product_date UNIQUE (machine_id, product_id, sales_date),
    CONSTRAINT chk_manual_sales_unit CHECK (quantity_unit IN ('GLASSES', 'ML')),
    CONSTRAINT chk_manual_sales_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_manual_sales_franchise_id ON manual_daily_flavour_sales(franchise_id);
CREATE INDEX idx_manual_sales_machine_id ON manual_daily_flavour_sales(machine_id);
CREATE INDEX idx_manual_sales_product_id ON manual_daily_flavour_sales(product_id);
CREATE INDEX idx_manual_sales_sales_date ON manual_daily_flavour_sales(sales_date DESC);
