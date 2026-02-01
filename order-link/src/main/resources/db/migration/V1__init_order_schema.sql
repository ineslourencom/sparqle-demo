DROP TABLE IF EXISTS inventory_entries;
DROP TABLE IF EXISTS order_request_entries;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE order_request_entries (
    order_request_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    merchant_id BIGINT NOT NULL CHECK (merchant_id > 0),
    merchant_ref VARCHAR(255) NOT NULL,
    barcode VARCHAR(16),
    recipient_name VARCHAR(255) NOT NULL,
    recipient_address1 VARCHAR(255) NOT NULL,
    recipient_address2 VARCHAR(255),
    recipient_postal_code VARCHAR(32) NOT NULL,
    recipient_city VARCHAR(128) NOT NULL,
    recipient_country VARCHAR(64) NOT NULL,
    parcel_weight_grams BIGINT NOT NULL CHECK (parcel_weight_grams > 0),
    parcel_length_cm NUMERIC(10, 2) NOT NULL CHECK (parcel_length_cm > 0),
    parcel_width_cm NUMERIC(10, 2) NOT NULL CHECK (parcel_width_cm > 0),
    parcel_height_cm NUMERIC(10, 2) NOT NULL CHECK (parcel_height_cm > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_order_request_barcode_format CHECK (barcode IS NULL OR barcode ~ '^[A-Z0-9]{10,16}$'),
    CONSTRAINT uq_order_request_merchant_ref UNIQUE (merchant_ref)
);

CREATE TABLE IF NOT EXISTS inventory_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_request_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    tracking_ref VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    details VARCHAR(1024),
    CONSTRAINT uq_inventory_tracking_ref UNIQUE (tracking_ref),
    CONSTRAINT uq_inventory_order_request UNIQUE (order_request_id),
    CONSTRAINT chk_inventory_status CHECK (status IN (
        'PENDING',
        'INVENTORY_RESERVED',
        'LOGISTICS_CONFIRMED',
        'COMPLETED',
        'FAILED_INVENTORY',
        'FAILED_LOGISTICS',
        'CANCELLED'
    )),
    CONSTRAINT fk_inventory_order_request FOREIGN KEY (order_request_id)
        REFERENCES order_request_entries (order_request_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_request_entries_merchant_ref
    ON order_request_entries (merchant_ref);

CREATE INDEX IF NOT EXISTS idx_inventory_entries_status
    ON inventory_entries (status);
