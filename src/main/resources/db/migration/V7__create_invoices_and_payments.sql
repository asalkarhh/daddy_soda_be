-- =============================================================================
-- Migration V7: Create invoices, invoice_items, and payment_collections tables
-- Module: Invoice, GST/Normal Bill, Credit Due Date, PDF & Payment Collection (BE-06)
-- =============================================================================

-- 1. invoices
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    sales_order_id UUID NOT NULL UNIQUE REFERENCES sales_orders(id),
    invoice_number VARCHAR(40) NOT NULL UNIQUE,
    invoice_type VARCHAR(20) NOT NULL CHECK (invoice_type IN ('GST', 'NORMAL')),
    invoice_date DATE NOT NULL,
    payment_term VARCHAR(20) NOT NULL CHECK (payment_term IN ('FULL_PAYMENT', 'PARTIAL_PAYMENT', 'CREDIT')),
    due_date DATE,
    subtotal_amount DECIMAL(14,2) NOT NULL,
    gst_rate_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    gst_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(14,2) NOT NULL,
    paid_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    balance_amount DECIMAL(14,2) NOT NULL,
    payment_status VARCHAR(30) NOT NULL CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'OVERDUE')),
    pdf_file_path VARCHAR(500),
    pdf_generated_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_invoices_sales_order_id ON invoices(sales_order_id);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_invoice_date ON invoices(invoice_date);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_payment_status ON invoices(payment_status);

-- 2. invoice_items
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    item_code VARCHAR(50) NOT NULL,
    flavour_name VARCHAR(150) NOT NULL,
    bottle_size_litre DECIMAL(10,2),
    quantity DECIMAL(14,3) NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12,2) NOT NULL CHECK (unit_price >= 0),
    line_total DECIMAL(14,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_invoice_items_invoice_product UNIQUE (invoice_id, product_id)
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);

-- 3. payment_collections
CREATE TABLE payment_collections (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    payment_date DATE NOT NULL,
    amount DECIMAL(14,2) NOT NULL CHECK (amount > 0),
    payment_mode VARCHAR(30) NOT NULL CHECK (payment_mode IN ('CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'OTHER')),
    reference_number VARCHAR(100),
    remarks TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID
);

CREATE INDEX idx_payment_collections_invoice_id ON payment_collections(invoice_id);
CREATE INDEX idx_payment_collections_payment_date ON payment_collections(payment_date);
