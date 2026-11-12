
CREATE TABLE careeats.customer_dietary_restrictions (
    user_id BIGINT NOT NULL,
    restriction VARCHAR(50) NOT NULL,

    PRIMARY KEY (user_id, restriction),

    CONSTRAINT fk_customer_dietary_user
        FOREIGN KEY (user_id)
        REFERENCES careeats.app_users(id)
        ON DELETE CASCADE
);
