-- Migration: V4__add_franchise_reference_to_users.sql
-- Module: User-Franchise Relationship Mapping

ALTER TABLE users ADD COLUMN IF NOT EXISTS franchise_id UUID NULL;

ALTER TABLE users 
    ADD CONSTRAINT fk_users_franchise 
    FOREIGN KEY (franchise_id) REFERENCES franchises(id);

CREATE INDEX IF NOT EXISTS idx_users_franchise_id ON users(franchise_id);
