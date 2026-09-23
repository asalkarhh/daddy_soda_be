-- =============================================================================
-- Migration V6: Create franchise sales orders and order items tables
-- Module: Franchise Sales Order, Delivery, and Warehouse Stock Deduction (BE-05)
-- =============================================================================

-- 1. sales_orders
CREATE TABLE sales_orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL UNIQUE,
    franchise_id UUID NOT NULL REFERENCES franchises(id),
    order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(30) NOT NULL,
    notes TEXT,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by UUID,
    prepared_at TIMESTAMP WITH TIME ZONE,
    prepared_by UUID,
    delivered_at TIMESTAMP WITH TIME ZONE,
    delivered_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_sales_orders_franchise_id ON sales_orders(franchise_id);
CREATE INDEX idx_sales_orders_status ON sales_orders(status);
CREATE INDEX idx_sales_orders_order_date ON sales_orders(order_date);
CREATE INDEX idx_sales_orders_order_number ON sales_orders(order_number);

-- 2. sales_order_items
CREATE TABLE sales_order_items (
    id UUID PRIMARY KEY,
    sales_order_id UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity DECIMAL(14,3) NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12,2) CHECK (unit_price >= 0),
    line_total DECIMAL(14,2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_sales_order_items_order_product UNIQUE (sales_order_id, product_id)
);

CREATE INDEX idx_sales_order_items_order_id ON sales_order_items(sales_order_id);
CREATE INDEX idx_sales_order_items_product_id ON sales_order_items(product_id);
