DELIMITER $$
-- audit_address_update, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_address_update
AFTER UPDATE ON address FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.street <=> NEW.street) THEN
        SET b = JSON_SET(b, '$.street', OLD.street), a = JSON_SET(a, '$.street', NEW.street);
    END IF;
    IF NOT (OLD.number <=> NEW.number) THEN
        SET b = JSON_SET(b, '$.number', OLD.number), a = JSON_SET(a, '$.number', NEW.number);
    END IF;
    IF NOT (OLD.neighborhood <=> NEW.neighborhood) THEN
        SET b = JSON_SET(b, '$.neighborhood', OLD.neighborhood), a = JSON_SET(a, '$.neighborhood', NEW.neighborhood);
    END IF;
    IF NOT (OLD.complement <=> NEW.complement) THEN
        SET b = JSON_SET(b, '$.complement', OLD.complement), a = JSON_SET(a, '$.complement', NEW.complement);
    END IF;
    IF NOT (OLD.zip_code <=> NEW.zip_code) THEN
        SET b = JSON_SET(b, '$.zip_code', OLD.zip_code), a = JSON_SET(a, '$.zip_code', NEW.zip_code);
    END IF;
    IF NOT (OLD.city <=> NEW.city) THEN
        SET b = JSON_SET(b, '$.city', OLD.city), a = JSON_SET(a, '$.city', NEW.city);
    END IF;
    IF NOT (OLD.state <=> NEW.state) THEN
        SET b = JSON_SET(b, '$.state', OLD.state), a = JSON_SET(a, '$.state', NEW.state);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('address', NEW.id, 'UPDATE', b, a, FALSE);
END$$
DELIMITER ;
