-- Migration: V3__create_master_data_tables.sql
-- Module: Master Data (Franchises, Outlets, Machines, Products, Suppliers)

-- 1. Franchises Table
CREATE TABLE IF NOT EXISTS franchises (
    id UUID PRIMARY KEY,
    franchise_code VARCHAR(30) NOT NULL UNIQUE,
    business_name VARCHAR(150) NOT NULL,
    owner_name VARCHAR(150) NOT NULL,
    mobile VARCHAR(15) NOT NULL,
    email VARCHAR(150) NULL,
    fssai_number VARCHAR(100) NULL,
    address TEXT NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(100) NULL,
    pincode VARCHAR(10) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX IF NOT EXISTS idx_franchises_code ON franchises(franchise_code);
CREATE INDEX IF NOT EXISTS idx_franchises_business_name ON franchises(business_name);
CREATE INDEX IF NOT EXISTS idx_franchises_is_active ON franchises(is_active);

-- 2. Outlets Table
CREATE TABLE IF NOT EXISTS outlets (
    id UUID PRIMARY KEY,
    franchise_id UUID NOT NULL,
    outlet_code VARCHAR(30) NOT NULL UNIQUE,
    outlet_name VARCHAR(150) NOT NULL,
    contact_person_name VARCHAR(150) NULL,
    contact_mobile VARCHAR(15) NULL,
    address TEXT NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(100) NULL,
    pincode VARCHAR(10) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    CONSTRAINT fk_outlets_franchise FOREIGN KEY (franchise_id) REFERENCES franchises(id)
);

CREATE INDEX IF NOT EXISTS idx_outlets_franchise_id ON outlets(franchise_id);
CREATE INDEX IF NOT EXISTS idx_outlets_code ON outlets(outlet_code);
CREATE INDEX IF NOT EXISTS idx_outlets_is_active ON outlets(is_active);

-- 3. Machines Table
CREATE TABLE IF NOT EXISTS machines (
    id UUID PRIMARY KEY,
    outlet_id UUID NOT NULL,
    machine_code VARCHAR(50) NOT NULL UNIQUE,
    machine_name VARCHAR(150) NULL,
    device_id VARCHAR(100) NULL UNIQUE,
    installation_date DATE NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    CONSTRAINT fk_machines_outlet FOREIGN KEY (outlet_id) REFERENCES outlets(id)
);

CREATE INDEX IF NOT EXISTS idx_machines_outlet_id ON machines(outlet_id);
CREATE INDEX IF NOT EXISTS idx_machines_code ON machines(machine_code);
CREATE INDEX IF NOT EXISTS idx_machines_device_id ON machines(device_id);
CREATE INDEX IF NOT EXISTS idx_machines_is_active ON machines(is_active);

-- 4. Products Table (Syrups / Flavours)
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    item_code VARCHAR(50) NOT NULL UNIQUE,
    flavour_name VARCHAR(150) NOT NULL,
    bottle_size_litre DECIMAL(10,2) NOT NULL DEFAULT 5.00,
    default_selling_price DECIMAL(12,2) NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NULL,
    updated_by UUID NULL,

    CONSTRAINT chk_products_bottle_size CHECK (bottle_size_litre > 0),
    CONSTRAINT chk_products_selling_price CHECK (default_selling_price IS NULL OR default_selling_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_products_item_code ON products(item_code);
CREATE INDEX IF NOT EXISTS idx_products_flavour_name ON products(flavour_name);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products(is_active);

-- 5. Suppliers Table
CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY,
    supplier_code VARCHAR(30) NOT NULL UNIQUE,
    supplier_name VARCHAR(150) NOT NULL,
    contact_person_name VARCHAR(150) NULL,
    mobile VARCHAR(15) NULL,
    email VARCHAR(150) NULL,
    gstin VARCHAR(20) NULL,
    address TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX IF NOT EXISTS idx_suppliers_code ON suppliers(supplier_code);
CREATE INDEX IF NOT EXISTS idx_suppliers_name ON suppliers(supplier_name);
CREATE INDEX IF NOT EXISTS idx_suppliers_is_active ON suppliers(is_active);
