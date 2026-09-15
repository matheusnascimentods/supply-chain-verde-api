CREATE TYPE product_category AS ENUM (
    'AGRICULTURE',
    'LIVESTOCK',
    'PROCESSED_FOOD',
    'TEXTILE',
    'FORESTRY',
    'OTHER'
);

CREATE TYPE product_unit AS ENUM (
    'KG',
    'TON',
    'LITER',
    'UNIT',
    'M3'
);

CREATE TABLE product (
    product_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    category product_category NOT NULL,
    unit product_unit NOT NULL,
    description TEXT
);

CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_product_name ON product (name);
