CREATE TYPE transport_mode AS ENUM (
    'ROAD',
    'RAIL',
    'MARITIME',
    'AIR'
);

CREATE TYPE fuel_type AS ENUM (
    'DIESEL',
    'BIODIESEL',
    'ELECTRIC',
    'GASOLINE',
    'ETHANOL',
    'AVIATION_KEROSENE',
    'HEAVY_FUEL_OIL'
);

CREATE TABLE transport (
    transport_id BIGSERIAL PRIMARY KEY,
    chain_id BIGINT NOT NULL UNIQUE,
    transport_mode transport_mode NOT NULL,
    distance NUMERIC(19, 4) NOT NULL,
    fuel_type fuel_type NOT NULL,
    capacity NUMERIC(19, 4) NOT NULL,
    CONSTRAINT fk_transport_chain
        FOREIGN KEY (chain_id)
        REFERENCES chain (chain_id),
    CONSTRAINT chk_transport_distance_positive
        CHECK (distance > 0),
    CONSTRAINT chk_transport_capacity_positive
        CHECK (capacity > 0)
);

CREATE INDEX idx_transport_chain_id ON transport (chain_id);
CREATE INDEX idx_transport_mode ON transport (transport_mode);
CREATE INDEX idx_transport_fuel_type ON transport (fuel_type);
