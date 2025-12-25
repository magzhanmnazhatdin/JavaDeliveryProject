CREATE TABLE favorite_restaurants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, restaurant_id)
);

CREATE INDEX idx_favorite_restaurants_user_id ON favorite_restaurants(user_id);
CREATE INDEX idx_favorite_restaurants_restaurant_id ON favorite_restaurants(restaurant_id);
