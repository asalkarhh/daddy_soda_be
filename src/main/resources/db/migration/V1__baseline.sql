-- Baseline migration: V1__baseline.sql
-- Project: Daddy Soda House Backend Foundation
-- Purpose: Initialize metadata tracking table and foundational database configuration

CREATE TABLE IF NOT EXISTS application_metadata (
    id VARCHAR(64) PRIMARY KEY,
    property_key VARCHAR(100) NOT NULL UNIQUE,
    property_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO application_metadata (id, property_key, property_value, description)
VALUES ('meta-001', 'application_name', 'Daddy Soda House Backend', 'Franchise management portal backend foundation');
