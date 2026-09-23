-- ==============================================================================
-- Daddy Soda House - Migration V9: IoT Machine Dispense & Heartbeat Events
-- ==============================================================================

CREATE TABLE iot_events (
    id UUID PRIMARY KEY,
    external_event_id VARCHAR(100) NOT NULL UNIQUE,
    machine_id UUID NOT NULL REFERENCES machines(id),
    franchise_id UUID NOT NULL REFERENCES franchises(id),
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    drink_name VARCHAR(150) NULL,
    event_status VARCHAR(30) NOT NULL,
    syrup_quantity_ml DECIMAL(14,3) NOT NULL DEFAULT 0,
    raw_soda_quantity_ml DECIMAL(14,3) NOT NULL DEFAULT 0,
    raw_payload JSONB NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_iot_event_type CHECK (event_type IN ('DRINK_DISPENSED', 'HEARTBEAT')),
    CONSTRAINT chk_iot_event_status CHECK (event_status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT chk_iot_syrup_quantity CHECK (syrup_quantity_ml >= 0),
    CONSTRAINT chk_iot_raw_soda_quantity CHECK (raw_soda_quantity_ml >= 0)
);

CREATE INDEX idx_iot_events_machine_id ON iot_events(machine_id);
CREATE INDEX idx_iot_events_franchise_id ON iot_events(franchise_id);
CREATE INDEX idx_iot_events_event_time ON iot_events(event_time DESC);
CREATE INDEX idx_iot_events_event_type ON iot_events(event_type);
CREATE INDEX idx_iot_events_received_at ON iot_events(received_at DESC);

CREATE TABLE iot_event_items (
    id UUID PRIMARY KEY,
    iot_event_id UUID NOT NULL REFERENCES iot_events(id) ON DELETE CASCADE,
    product_id UUID NULL REFERENCES products(id),
    item_type VARCHAR(30) NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(150) NULL,
    line_no INTEGER NULL,
    quantity_ml DECIMAL(14,3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_iot_item_type CHECK (item_type IN ('SYRUP', 'SODA')),
    CONSTRAINT chk_iot_item_quantity_ml CHECK (quantity_ml > 0)
);

CREATE INDEX idx_iot_event_items_event_id ON iot_event_items(iot_event_id);
CREATE INDEX idx_iot_event_items_product_id ON iot_event_items(product_id);
CREATE INDEX idx_iot_event_items_item_type ON iot_event_items(item_type);
CREATE INDEX idx_iot_event_items_item_code ON iot_event_items(item_code);
