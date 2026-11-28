DELIMITER $$
-- audit_carbon_emission_insert, from V22__fatores_de_emissao.sql.
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
DELIMITER ;
