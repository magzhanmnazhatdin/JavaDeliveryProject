-- V2__create_deliveries_table.sql
-- Deliveries table for tracking order deliveries

CREATE TABLE deliveries (
    id                  UUID PRIMARY KEY,
    order_id            UUID NOT NULL UNIQUE,
    customer_id         UUID NOT NULL,
    restaurant_id       UUID NOT NULL,
    courier_id          UUID REFERENCES couriers(id),

    -- Delivery address
    delivery_address    VARCHAR(500) NOT NULL,
    delivery_lat        DECIMAL(10, 8),
    delivery_lng        DECIMAL(11, 8),

    -- Restaurant address (for pickup)
    pickup_address      VARCHAR(500),
    pickup_lat          DECIMAL(10, 8),
    pickup_lng          DECIMAL(11, 8),

    -- Status tracking
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    -- Timestamps
    assigned_at         TIMESTAMP WITH TIME ZONE,
    picked_up_at        TIMESTAMP WITH TIME ZONE,
    delivered_at        TIMESTAMP WITH TIME ZONE,
    cancelled_at        TIMESTAMP WITH TIME ZONE,

    -- Additional info
    cancellation_reason VARCHAR(500),
    customer_notes      VARCHAR(500),
    courier_notes       VARCHAR(500),

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for finding deliveries by order
CREATE INDEX idx_deliveries_order_id ON deliveries(order_id);

-- Index for finding deliveries by courier
CREATE INDEX idx_deliveries_courier_id ON deliveries(courier_id);

-- Index for finding deliveries by status
CREATE INDEX idx_deliveries_status ON deliveries(status);

-- Index for finding deliveries by customer
CREATE INDEX idx_deliveries_customer_id ON deliveries(customer_id);

COMMENT ON TABLE deliveries IS 'Order deliveries tracking';
COMMENT ON COLUMN deliveries.status IS 'PENDING, COURIER_ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, CANCELLED';
