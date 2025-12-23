-- V1__create_restaurants_table.sql
-- Restaurants table

CREATE TABLE restaurants (
    id              UUID PRIMARY KEY,
    keycloak_id     VARCHAR(255) UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    address         VARCHAR(500) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    phone           VARCHAR(50),
    email           VARCHAR(255),

    -- Location coordinates
    latitude        DECIMAL(10, 8),
    longitude       DECIMAL(11, 8),

    -- Operating info
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    opening_time    TIME,
    closing_time    TIME,

    -- Rating
    average_rating  DECIMAL(3, 2) DEFAULT 0.00,
    total_reviews   INTEGER DEFAULT 0,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_restaurants_city ON restaurants(city);
CREATE INDEX idx_restaurants_is_active ON restaurants(is_active);
CREATE INDEX idx_restaurants_keycloak_id ON restaurants(keycloak_id);

COMMENT ON TABLE restaurants IS 'Restaurant information';
COMMENT ON COLUMN restaurants.keycloak_id IS 'Reference to Keycloak user ID for restaurant owner';
