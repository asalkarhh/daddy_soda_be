-- =============================================================================
-- Migration V20: Add agreement file storage columns to outlets
-- Module: Outlet Management / Franchise Agreement Attachment
-- =============================================================================

ALTER TABLE outlets
    ADD COLUMN IF NOT EXISTS agreement_file_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS agreement_file_path VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS agreement_file_type VARCHAR(100) NULL;

CREATE INDEX IF NOT EXISTS idx_outlets_has_agreement ON outlets(agreement_file_path) WHERE agreement_file_path IS NOT NULL;
