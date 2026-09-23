-- Migration: V2__create_users_and_roles.sql
-- Module: Authentication, Users, Roles & Security
-- Purpose: Create users table with role constraint, 10-digit mobile rule, and audit fields.

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    mobile VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(150) NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    CONSTRAINT chk_users_role CHECK (role IN ('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')),
    CONSTRAINT chk_users_mobile_len CHECK (LENGTH(mobile) = 10)
);

-- Indexes for fast lookup by mobile, role filtering, and active status
CREATE INDEX IF NOT EXISTS idx_users_mobile ON users(mobile);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);
