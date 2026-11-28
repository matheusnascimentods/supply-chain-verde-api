DELIMITER $$
-- audit_carbon_emission_update, from V22__fatores_de_emissao.sql.
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
DELIMITER ;
