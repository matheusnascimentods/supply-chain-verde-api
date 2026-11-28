DELIMITER $$
-- audit_carbon_emission_delete, from V22__fatores_de_emissao.sql.
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
