-- V1__create_couriers_table.sql
-- Couriers table for delivery personnel

CREATE TABLE couriers (
    id              UUID PRIMARY KEY,
    keycloak_id     VARCHAR(255) UNIQUE,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(50) NOT NULL,
    email           VARCHAR(255),
    status          VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    current_location_lat DECIMAL(10, 8),
    current_location_lng DECIMAL(11, 8),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for finding available couriers
CREATE INDEX idx_couriers_status ON couriers(status);

-- Index for keycloak lookup
CREATE INDEX idx_couriers_keycloak_id ON couriers(keycloak_id);

COMMENT ON TABLE couriers IS 'Delivery personnel / couriers';
COMMENT ON COLUMN couriers.status IS 'AVAILABLE, BUSY, OFFLINE';
COMMENT ON COLUMN couriers.keycloak_id IS 'Reference to Keycloak user ID';
