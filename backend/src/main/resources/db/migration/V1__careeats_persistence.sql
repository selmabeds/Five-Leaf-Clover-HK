
CREATE SEQUENCE careeats.user_ids START WITH 1000;
CREATE SEQUENCE careeats.order_ids START WITH 1000;
CREATE SEQUENCE careeats.acceptance_ids START WITH 1;

-- Transaction-scoped, cross-process mutex for aggregate mutations and seed execution.
-- Conservative serialization is intentional for this small shared demonstration database.
CREATE TABLE careeats.write_lock (id INTEGER PRIMARY KEY CHECK (id = 1));
INSERT INTO careeats.write_lock (id) VALUES (1);
CREATE TABLE careeats.seed_runs (name VARCHAR(100) PRIMARY KEY, completed_at TIMESTAMP WITH TIME ZONE NOT NULL);

CREATE TABLE careeats.app_users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER','RESTAURANT','COURIER','OPERATOR')),
    CHECK (email = LOWER(TRIM(email)))
);
CREATE TABLE careeats.restaurants (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','PAUSED'))
);
CREATE TABLE careeats.restaurant_assignments (
    user_id BIGINT PRIMARY KEY REFERENCES careeats.app_users(id),
    restaurant_id BIGINT NOT NULL REFERENCES careeats.restaurants(id)
);
CREATE TABLE careeats.menu_items (
    restaurant_id BIGINT NOT NULL REFERENCES careeats.restaurants(id),
    item_id BIGINT NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    name VARCHAR(255) NOT NULL,
    price NUMERIC(19,2) NOT NULL CHECK (price >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE','SOLD_OUT')),
    PRIMARY KEY (restaurant_id,item_id),
    UNIQUE (restaurant_id,position)
);
CREATE TABLE careeats.menu_ingredients (
    restaurant_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    ingredient VARCHAR(255) NOT NULL,
    PRIMARY KEY (restaurant_id,item_id,position),
    FOREIGN KEY (restaurant_id,item_id) REFERENCES careeats.menu_items(restaurant_id,item_id)
);
CREATE TABLE careeats.orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES careeats.app_users(id),
    restaurant_id BIGINT NOT NULL REFERENCES careeats.restaurants(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PLACED','ACCEPTED','PREPARING','READY','REJECTED','CANCELLED')),
    rejection_reason VARCHAR(2000),
    accepted_sequence BIGINT UNIQUE,
    UNIQUE (id,restaurant_id),
    CHECK (status <> 'ACCEPTED' OR accepted_sequence IS NOT NULL)
);
CREATE INDEX orders_customer_idx ON careeats.orders(customer_id,id);
CREATE INDEX orders_fifo_idx ON careeats.orders(restaurant_id,status,accepted_sequence);
CREATE TABLE careeats.order_lines (
    order_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19,2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (order_id,position),
    UNIQUE (order_id,item_id),
    FOREIGN KEY (order_id,restaurant_id) REFERENCES careeats.orders(id,restaurant_id),
    FOREIGN KEY (restaurant_id,item_id) REFERENCES careeats.menu_items(restaurant_id,item_id)
);
CREATE TABLE careeats.order_transitions (
    event_id VARCHAR(36) PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES careeats.orders(id),
    position INTEGER NOT NULL CHECK (position >= 0),
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    UNIQUE (order_id,position),
    CHECK (previous_status IS NOT NULL OR (position = 0 AND new_status = 'PLACED'))
);
CREATE TABLE careeats.restaurant_transitions (
    event_id VARCHAR(36) PRIMARY KEY,
    restaurant_id BIGINT NOT NULL REFERENCES careeats.restaurants(id),
    position INTEGER NOT NULL CHECK (position >= 0),
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    UNIQUE (restaurant_id,position),
    CHECK (previous_status <> new_status)
);
CREATE TABLE careeats.menu_transitions (
    event_id VARCHAR(36) PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    UNIQUE (restaurant_id,item_id,position),
    FOREIGN KEY (restaurant_id,item_id) REFERENCES careeats.menu_items(restaurant_id,item_id),
    CHECK (previous_status <> new_status)
);

-- PRC's delivered-order input is a financial snapshot, not a DLV implementation.
-- It references a real ORD order; a future DLV integration supplies completion facts.
CREATE TABLE careeats.delivered_orders (
    order_id BIGINT PRIMARY KEY REFERENCES careeats.orders(id),
    customer_id BIGINT NOT NULL REFERENCES careeats.app_users(id),
    restaurant_id BIGINT NOT NULL REFERENCES careeats.restaurants(id),
    courier_id BIGINT REFERENCES careeats.app_users(id),
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (delivered_at >= placed_at)
);
CREATE TABLE careeats.ledger_entries (
    order_id BIGINT NOT NULL REFERENCES careeats.delivered_orders(order_id),
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('CHARGE','REFUND')),
    order_state VARCHAR(20) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount >= 0),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (order_id,entry_type),
    CHECK ((entry_type='CHARGE' AND order_state='DELIVERED') OR
           (entry_type='REFUND' AND order_state='REFUNDED'))
);
CREATE TABLE careeats.fare_snapshots (
    order_id BIGINT PRIMARY KEY REFERENCES careeats.delivered_orders(order_id),
    item_subtotal NUMERIC(19,2) NOT NULL CHECK (item_subtotal >= 0),
    delivery_fee NUMERIC(19,2) NOT NULL CHECK (delivery_fee >= 0),
    service_fee NUMERIC(19,2) NOT NULL CHECK (service_fee >= 0),
    small_order_surcharge NUMERIC(19,2) NOT NULL CHECK (small_order_surcharge >= 0),
    total_charge NUMERIC(19,2) NOT NULL,
    CHECK (total_charge = item_subtotal + delivery_fee + service_fee + small_order_surcharge)
);
CREATE TABLE careeats.priced_lines (
    order_id BIGINT NOT NULL REFERENCES careeats.delivered_orders(order_id),
    position INTEGER NOT NULL CHECK (position >= 0),
    item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19,2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (order_id,position)
);
