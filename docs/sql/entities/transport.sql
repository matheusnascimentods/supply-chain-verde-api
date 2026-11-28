CREATE TABLE transport (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain_id       BIGINT NOT NULL,
    transport_mode ENUM('ROAD', 'RAIL', 'MARITIME', 'AIR') COLLATE utf8mb4_0900_as_ci NOT NULL,
    distance       DECIMAL(19,4) NOT NULL,
    fuel_type      ENUM('DIESEL', 'BIODIESEL', 'ELECTRIC', 'GASOLINE', 'ETHANOL', 'AVIATION_KEROSENE', 'HEAVY_FUEL_OIL')
                   COLLATE utf8mb4_0900_as_ci NOT NULL,
    capacity       DECIMAL(19,4) NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT transport_chain_id_key UNIQUE (chain_id),
    CONSTRAINT chk_transport_capacity_positive CHECK (capacity > 0),
    CONSTRAINT chk_transport_distance_positive CHECK (distance > 0),
    CONSTRAINT fk_transport_chain FOREIGN KEY (chain_id) REFERENCES chain (id),
    INDEX idx_transport_fuel_type (fuel_type),
    INDEX idx_transport_mode (transport_mode)
);
