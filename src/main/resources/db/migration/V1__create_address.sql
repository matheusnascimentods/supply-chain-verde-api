CREATE TABLE address (
    address_id BIGSERIAL PRIMARY KEY,
    street VARCHAR(255) NOT NULL,
    number VARCHAR(30) NOT NULL,
    neighborhood VARCHAR(120) NOT NULL,
    complement VARCHAR(120),
    zip_code VARCHAR(20) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state VARCHAR(2) NOT NULL
);

CREATE INDEX idx_address_city_state ON address (city, state);
