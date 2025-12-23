-- V4__create_restaurant_order_items_table.sql
-- Items within restaurant orders

CREATE TABLE restaurant_order_items (
    id                  UUID PRIMARY KEY,
    restaurant_order_id UUID NOT NULL REFERENCES restaurant_orders(id) ON DELETE CASCADE,
    menu_item_id        UUID NOT NULL,

    -- Snapshot of menu item at order time
    name_snapshot       VARCHAR(255) NOT NULL,
    price_snapshot      DECIMAL(10, 2) NOT NULL,

    quantity            INTEGER NOT NULL DEFAULT 1,
    special_instructions VARCHAR(500),

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_restaurant_order_items_order_id ON restaurant_order_items(restaurant_order_id);
CREATE INDEX idx_restaurant_order_items_menu_item_id ON restaurant_order_items(menu_item_id);

COMMENT ON TABLE restaurant_order_items IS 'Individual items in a restaurant order';
