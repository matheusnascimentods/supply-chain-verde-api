CREATE TYPE calculation_method AS ENUM (
    'DEFRA',
    'GHG_PROTOCOL',
    'IPCC',
    'EMEP_EEA'
);

CREATE TABLE carbon_emission (
    emission_id BIGSERIAL PRIMARY KEY,
    chain_id BIGINT NOT NULL UNIQUE,
    emission_factor NUMERIC(19, 8) NOT NULL,
    co2_kg NUMERIC(19, 4) NOT NULL,
    calculation_method calculation_method NOT NULL,
    calculated_at DATE NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT fk_carbon_emission_chain
        FOREIGN KEY (chain_id)
        REFERENCES chain (chain_id),
    CONSTRAINT chk_carbon_emission_factor_non_negative
        CHECK (emission_factor >= 0),
    CONSTRAINT chk_carbon_emission_co2_non_negative
        CHECK (co2_kg >= 0)
);

CREATE INDEX idx_carbon_emission_chain_id ON carbon_emission (chain_id);
CREATE INDEX idx_carbon_emission_calculated_at ON carbon_emission (calculated_at);
CREATE INDEX idx_carbon_emission_calculation_method ON carbon_emission (calculation_method);
