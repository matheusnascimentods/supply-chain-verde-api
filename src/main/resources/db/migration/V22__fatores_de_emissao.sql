-- Feature 019: fator por atividade. Os fatores são dados versionados por migration (RN-02).
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

GRANT SELECT ON emission_factor TO '${app_user}'@'%';

-- ponytail: aéreo com uma faixa só (500 a 3700 km), que cobre os voos domésticos; faixas por distância se aparecer frete internacional.
-- ponytail: combustível estacionário do GHG Protocol usa o CH4 do setor Manufatura; o Comercial difere 0,3%. Separar se o score precisar.
-- ponytail: diesel B15 é a mistura de ago/2025 em diante (média de 2025 foi 14,42%). Nova migration com UPDATE quando a mistura mudar.

INSERT INTO emission_factor
    (calculation_method, transport_mode, fuel_type, energy_source, value, unit, source, reference_year)
VALUES
    ('DEFRA', 'ROAD', 'DIESEL', NULL, 0.10356, 'kgCO2e/tkm',
     'DESNZ 2026, HGV all diesel, average laden', 2026),
    ('DEFRA', 'ROAD', 'GASOLINE', NULL, 0.83810, 'kgCO2e/tkm',
     'DESNZ 2026, vans up to 3.5 t, petrol, average', 2026),
    ('DEFRA', 'ROAD', 'ELECTRIC', NULL, 0.14699, 'kgCO2e/tkm',
     'DESNZ 2026, vans up to 3.5 t, battery electric, average', 2026),
    ('DEFRA', 'RAIL', 'DIESEL', NULL, 0.02583, 'kgCO2e/tkm',
     'DESNZ 2026, freight train, average fleet', 2026),
    ('DEFRA', 'MARITIME', 'HEAVY_FUEL_OIL', NULL, 0.01321, 'kgCO2e/tkm',
     'DESNZ 2026, general cargo ship, average', 2026),
    ('DEFRA', 'AIR', 'AVIATION_KEROSENE', NULL, 0.75539, 'kgCO2e/tkm',
     'DESNZ 2026, short-haul freight flight, without RF', 2026),
    ('GHG_PROTOCOL', 'ROAD', 'DIESEL', NULL, 0.08967, 'kgCO2e/tkm',
     'Programa Brasileiro GHG Protocol, caminhão médio, B15', 2026),
    ('GHG_PROTOCOL', 'ROAD', 'BIODIESEL', NULL, 0.00136, 'kgCO2e/tkm',
     'Programa Brasileiro GHG Protocol, B100, sem CO2 biogênico', 2026),
    ('GHG_PROTOCOL', 'RAIL', 'DIESEL', NULL, 0.00967, 'kgCO2e/tkm',
     'Programa Brasileiro GHG Protocol, média nacional ferroviária', 2026),
    ('GHG_PROTOCOL', 'MARITIME', 'DIESEL', NULL, 0.01321, 'kgCO2e/tkm',
     'Programa Brasileiro GHG Protocol, carga geral média', 2026),
    ('GHG_PROTOCOL', 'AIR', 'AVIATION_KEROSENE', NULL, 0.75539, 'kgCO2e/tkm',
     'Programa Brasileiro GHG Protocol, 500 a 3700 km', 2026),
    ('DEFRA', NULL, NULL, 'DIESEL', 2.58354, 'kgCO2e/L',
     'DESNZ 2026, diesel liquid fuel, average biofuel blend', 2026),
    ('DEFRA', NULL, NULL, 'LPG', 2.93936, 'kgCO2e/kg',
     'DESNZ 2026, LPG gaseous fuel', 2026),
    ('DEFRA', NULL, NULL, 'NATURAL_GAS', 2.02633, 'kgCO2e/m3',
     'DESNZ 2026, natural gas, cubic metres', 2026),
    ('GHG_PROTOCOL', NULL, NULL, 'ELECTRICITY', 0.04610, 'kgCO2/kWh',
     'MCTI/SIRENE, fator médio anual do SIN', 2025),
    ('GHG_PROTOCOL', NULL, NULL, 'DIESEL', 2.24482, 'kgCO2e/L',
     'Programa Brasileiro GHG Protocol, setor Manufatura, B15', 2026),
    ('GHG_PROTOCOL', NULL, NULL, 'LPG', 2.93346, 'kgCO2e/kg',
     'Programa Brasileiro GHG Protocol, setor Manufatura', 2026),
    ('GHG_PROTOCOL', NULL, NULL, 'NATURAL_GAS', 2.06895, 'kgCO2e/m3',
     'Programa Brasileiro GHG Protocol, setor Manufatura', 2026);

-- RN-17: o legado muda antes de reduzir o enum, para a troca ficar registrada pelas triggers.
UPDATE carbon_emission
SET calculation_method = 'GHG_PROTOCOL'
WHERE calculation_method IN ('IPCC', 'EMEP_EEA');

ALTER TABLE carbon_emission
    MODIFY calculation_method ENUM('DEFRA', 'GHG_PROTOCOL') COLLATE utf8mb4_0900_as_ci NOT NULL,
    ADD COLUMN cargo_weight_kg DECIMAL(19,4) NULL AFTER co2_kg,
    ADD COLUMN energy_source ENUM('ELECTRICITY', 'DIESEL', 'LPG', 'NATURAL_GAS') COLLATE utf8mb4_0900_as_ci NULL AFTER cargo_weight_kg,
    ADD COLUMN consumption DECIMAL(19,4) NULL AFTER energy_source,
    ADD CONSTRAINT chk_carbon_emission_cargo_weight_positive CHECK (cargo_weight_kg IS NULL OR cargo_weight_kg > 0),
    ADD CONSTRAINT chk_carbon_emission_consumption_non_negative CHECK (consumption IS NULL OR consumption >= 0);

-- RN-18: peso só é preenchido nos transportes antigos de produto em KG ou TON.
UPDATE carbon_emission ce
JOIN chain c ON c.id = ce.chain_id
JOIN batch b ON b.id = c.batch_id
JOIN product p ON p.id = b.product_id
SET ce.cargo_weight_kg = CASE p.unit
    WHEN 'KG' THEN b.quantity
    WHEN 'TON' THEN b.quantity * 1000
END
WHERE c.stage_type = 'TRANSPORT'
  AND p.unit IN ('KG', 'TON');

DROP TRIGGER audit_carbon_emission_insert;
DROP TRIGGER audit_carbon_emission_update;
DROP TRIGGER audit_carbon_emission_delete;

DELIMITER $$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_carbon_emission_insert
AFTER INSERT ON carbon_emission FOR EACH ROW
CALL audit_write('carbon_emission', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'chain_id', NEW.chain_id,
        'emission_factor', NEW.emission_factor,
        'co2_kg', NEW.co2_kg,
        'calculation_method', NEW.calculation_method,
        'cargo_weight_kg', NEW.cargo_weight_kg,
        'energy_source', NEW.energy_source,
        'consumption', NEW.consumption,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_carbon_emission_update
AFTER UPDATE ON carbon_emission FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.chain_id <=> NEW.chain_id) THEN
        SET b = JSON_SET(b, '$.chain_id', OLD.chain_id), a = JSON_SET(a, '$.chain_id', NEW.chain_id);
    END IF;
    IF NOT (OLD.emission_factor <=> NEW.emission_factor) THEN
        SET b = JSON_SET(b, '$.emission_factor', OLD.emission_factor), a = JSON_SET(a, '$.emission_factor', NEW.emission_factor);
    END IF;
    IF NOT (OLD.co2_kg <=> NEW.co2_kg) THEN
        SET b = JSON_SET(b, '$.co2_kg', OLD.co2_kg), a = JSON_SET(a, '$.co2_kg', NEW.co2_kg);
    END IF;
    IF NOT (OLD.calculation_method <=> NEW.calculation_method) THEN
        SET b = JSON_SET(b, '$.calculation_method', OLD.calculation_method), a = JSON_SET(a, '$.calculation_method', NEW.calculation_method);
    END IF;
    IF NOT (OLD.cargo_weight_kg <=> NEW.cargo_weight_kg) THEN
        SET b = JSON_SET(b, '$.cargo_weight_kg', OLD.cargo_weight_kg), a = JSON_SET(a, '$.cargo_weight_kg', NEW.cargo_weight_kg);
    END IF;
    IF NOT (OLD.energy_source <=> NEW.energy_source) THEN
        SET b = JSON_SET(b, '$.energy_source', OLD.energy_source), a = JSON_SET(a, '$.energy_source', NEW.energy_source);
    END IF;
    IF NOT (OLD.consumption <=> NEW.consumption) THEN
        SET b = JSON_SET(b, '$.consumption', OLD.consumption), a = JSON_SET(a, '$.consumption', NEW.consumption);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('carbon_emission', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_carbon_emission_delete
AFTER DELETE ON carbon_emission FOR EACH ROW
CALL audit_write('carbon_emission', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'chain_id', OLD.chain_id,
        'emission_factor', OLD.emission_factor,
        'co2_kg', OLD.co2_kg,
        'calculation_method', OLD.calculation_method,
        'cargo_weight_kg', OLD.cargo_weight_kg,
        'energy_source', OLD.energy_source,
        'consumption', OLD.consumption,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

DELIMITER ;
