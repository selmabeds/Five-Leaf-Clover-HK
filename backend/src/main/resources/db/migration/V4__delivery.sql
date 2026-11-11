CREATE TABLE careeats.couriers (
    id BIGINT PRIMARY KEY REFERENCES careeats.app_users(id),
    latitude DOUBLE PRECISION NOT NULL
        CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL
        CHECK (longitude BETWEEN -180 AND 180),
    state VARCHAR(10) NOT NULL
        CHECK (state IN ('OFFLINE', 'IDLE', 'ASSIGNED', 'CARRYING'))
);

CREATE TABLE careeats.delivery_assignments (
    order_id BIGINT PRIMARY KEY REFERENCES careeats.orders(id),
    courier_id BIGINT NOT NULL UNIQUE REFERENCES careeats.couriers(id),
    status VARCHAR(10) NOT NULL
        CHECK (status IN ('OFFERED', 'ACCEPTED', 'STALLED'))
);