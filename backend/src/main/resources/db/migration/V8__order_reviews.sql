CREATE TABLE careeats.order_reviews (
    order_id BIGINT PRIMARY KEY REFERENCES careeats.delivered_orders(order_id) ON DELETE CASCADE,
    restaurant_rating INTEGER NOT NULL CHECK (restaurant_rating BETWEEN 1 AND 5),
    courier_rating INTEGER NOT NULL CHECK (courier_rating BETWEEN 1 AND 5),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
