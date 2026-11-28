DELIMITER $$
-- audit_transport_update, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_transport_update
AFTER UPDATE ON transport FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.chain_id <=> NEW.chain_id) THEN
        SET b = JSON_SET(b, '$.chain_id', OLD.chain_id), a = JSON_SET(a, '$.chain_id', NEW.chain_id);
    END IF;
    IF NOT (OLD.transport_mode <=> NEW.transport_mode) THEN
        SET b = JSON_SET(b, '$.transport_mode', OLD.transport_mode), a = JSON_SET(a, '$.transport_mode', NEW.transport_mode);
    END IF;
    IF NOT (OLD.distance <=> NEW.distance) THEN
        SET b = JSON_SET(b, '$.distance', OLD.distance), a = JSON_SET(a, '$.distance', NEW.distance);
    END IF;
    IF NOT (OLD.fuel_type <=> NEW.fuel_type) THEN
        SET b = JSON_SET(b, '$.fuel_type', OLD.fuel_type), a = JSON_SET(a, '$.fuel_type', NEW.fuel_type);
    END IF;
    IF NOT (OLD.capacity <=> NEW.capacity) THEN
        SET b = JSON_SET(b, '$.capacity', OLD.capacity), a = JSON_SET(a, '$.capacity', NEW.capacity);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('transport', NEW.id, 'UPDATE', b, a, FALSE);
END$$
DELIMITER ;
