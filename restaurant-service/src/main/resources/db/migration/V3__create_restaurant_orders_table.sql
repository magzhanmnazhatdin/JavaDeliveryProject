-- V3__create_restaurant_orders_table.sql
-- Restaurant orders - tracking orders from Order Service perspective

CREATE TABLE restaurant_orders (
    id                  UUID PRIMARY KEY,
    order_id            UUID NOT NULL UNIQUE,
    restaurant_id       UUID NOT NULL REFERENCES restaurants(id),
    customer_id         UUID NOT NULL,

    -- Order details
    total_price         DECIMAL(10, 2) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    -- Delivery info (copied from order for convenience)
    delivery_address    VARCHAR(500),
    customer_notes      VARCHAR(500),

    -- Timestamps
    received_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    accepted_at         TIMESTAMP WITH TIME ZONE,
    rejected_at         TIMESTAMP WITH TIME ZONE,
    preparing_at        TIMESTAMP WITH TIME ZONE,
    ready_at            TIMESTAMP WITH TIME ZONE,

    -- Rejection info
    rejection_reason    VARCHAR(500),

    -- Estimated times
    estimated_prep_time_minutes INTEGER,

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_restaurant_orders_order_id ON restaurant_orders(order_id);
CREATE INDEX idx_restaurant_orders_restaurant_id ON restaurant_orders(restaurant_id);
CREATE INDEX idx_restaurant_orders_status ON restaurant_orders(status);
CREATE INDEX idx_restaurant_orders_customer_id ON restaurant_orders(customer_id);

COMMENT ON TABLE restaurant_orders IS 'Orders received by restaurant from Order Service';
COMMENT ON COLUMN restaurant_orders.status IS 'PENDING, ACCEPTED, REJECTED, PREPARING, READY, PICKED_UP, CANCELLED';
