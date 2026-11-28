CREATE TABLE emission_factor (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    calculation_method ENUM('DEFRA', 'GHG_PROTOCOL') COLLATE utf8mb4_0900_as_ci NOT NULL,
    transport_mode     ENUM('ROAD', 'RAIL', 'MARITIME', 'AIR') COLLATE utf8mb4_0900_as_ci NULL,
    fuel_type          ENUM('DIESEL', 'BIODIESEL', 'ELECTRIC', 'GASOLINE', 'ETHANOL', 'AVIATION_KEROSENE', 'HEAVY_FUEL_OIL')
                       COLLATE utf8mb4_0900_as_ci NULL,
    energy_source      ENUM('ELECTRICITY', 'DIESEL', 'LPG', 'NATURAL_GAS') COLLATE utf8mb4_0900_as_ci NULL,
    value              DECIMAL(19,8) NOT NULL,
    unit               VARCHAR(20) NOT NULL,
    source             VARCHAR(255) NOT NULL,
    reference_year     SMALLINT NOT NULL,
    CONSTRAINT chk_emission_factor_activity CHECK (
        (transport_mode IS NOT NULL AND fuel_type IS NOT NULL AND energy_source IS NULL)
        OR (transport_mode IS NULL AND fuel_type IS NULL AND energy_source IS NOT NULL)),
    CONSTRAINT chk_emission_factor_value_non_negative CHECK (value >= 0),
    CONSTRAINT uq_emission_factor UNIQUE (calculation_method, transport_mode, fuel_type, energy_source)
);
