-- =============================================================================
-- Migration V5: Create purchase and warehouse inventory tables
-- Module: Purchase Bill, Warehouse Inventory, and Stock Movement (BE-04)
-- =============================================================================

-- 1. purchase_entries
CREATE TABLE purchase_entries (
    id UUID PRIMARY KEY,
    purchase_number VARCHAR(40) NOT NULL UNIQUE,
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    supplier_bill_number VARCHAR(100),
    bill_date DATE NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    cancelled_reason TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_purchase_entries_supplier_id ON purchase_entries(supplier_id);
CREATE INDEX idx_purchase_entries_bill_date ON purchase_entries(bill_date);
CREATE INDEX idx_purchase_entries_status ON purchase_entries(status);
CREATE INDEX idx_purchase_entries_number ON purchase_entries(purchase_number);

-- Index on supplier_id and supplier_bill_number for lookup and duplicate check
CREATE INDEX idx_purchase_entries_supplier_bill ON purchase_entries (supplier_id, supplier_bill_number);

-- 2. purchase_items
CREATE TABLE purchase_items (
    id UUID PRIMARY KEY,
    purchase_entry_id UUID NOT NULL REFERENCES purchase_entries(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity DECIMAL(14,3) NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_purchase_items_entry_product UNIQUE (purchase_entry_id, product_id)
);

CREATE INDEX idx_purchase_items_entry_id ON purchase_items(purchase_entry_id);
CREATE INDEX idx_purchase_items_product_id ON purchase_items(product_id);

-- 3. inventory_stock
CREATE TABLE inventory_stock (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE REFERENCES products(id),
    available_quantity DECIMAL(14,3) NOT NULL DEFAULT 0.000 CHECK (available_quantity >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_inventory_stock_product_id ON inventory_stock(product_id);

-- 4. stock_movements
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    movement_type VARCHAR(30) NOT NULL,
    quantity_in DECIMAL(14,3) NOT NULL DEFAULT 0.000,
    quantity_out DECIMAL(14,3) NOT NULL DEFAULT 0.000,
    balance_after DECIMAL(14,3) NOT NULL CHECK (balance_after >= 0),
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID,
    reference_number VARCHAR(100),
    note TEXT,
    movement_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    CONSTRAINT chk_stock_movements_qty CHECK (
        (quantity_in > 0 AND quantity_out = 0) OR
        (quantity_in = 0 AND quantity_out > 0)
    )
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_movement_type ON stock_movements(movement_type);
CREATE INDEX idx_stock_movements_movement_at ON stock_movements(movement_at);
CREATE INDEX idx_stock_movements_reference_id ON stock_movements(reference_id);
