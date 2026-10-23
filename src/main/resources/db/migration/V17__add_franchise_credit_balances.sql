ALTER TABLE invoices ADD COLUMN original_total_amount DECIMAL(14,2) NULL;
ALTER TABLE invoices ADD COLUMN credit_applied_amount DECIMAL(14,2) NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN excess_credit_amount DECIMAL(14,2) NOT NULL DEFAULT 0;

CREATE TABLE franchise_credit_balances (
    id UUID PRIMARY KEY,
    franchise_id UUID NOT NULL UNIQUE,
    balance_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_franchise_credit_franchise FOREIGN KEY (franchise_id) REFERENCES franchises(id)
);
