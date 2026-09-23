-- ==============================================================================
-- Daddy Soda House - Migration V8: Public Enquiries
-- ==============================================================================

CREATE TABLE enquiries (
    id UUID PRIMARY KEY,
    enquiry_number VARCHAR(40) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    mobile VARCHAR(15) NOT NULL,
    email VARCHAR(150) NULL,
    city VARCHAR(100) NULL,
    message TEXT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'WEBSITE',
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    admin_notes TEXT NULL,
    contacted_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    CONSTRAINT chk_enquiries_status CHECK (status IN ('NEW', 'CONTACTED', 'CLOSED')),
    CONSTRAINT chk_enquiries_mobile CHECK (mobile ~ '^[0-9]{10}$'),
    CONSTRAINT chk_enquiries_message CHECK (message IS NULL OR length(message) <= 1000)
);

CREATE INDEX idx_enquiries_status ON enquiries(status);
CREATE INDEX idx_enquiries_mobile ON enquiries(mobile);
CREATE INDEX idx_enquiries_created_at ON enquiries(created_at);
CREATE INDEX idx_enquiries_enquiry_number ON enquiries(enquiry_number);
