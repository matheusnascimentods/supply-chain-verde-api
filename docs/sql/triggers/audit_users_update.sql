DELIMITER $$
-- audit_users_update, from V17__users_is_active.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_users_update
AFTER UPDATE ON users FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.name <=> NEW.name) THEN
        SET b = JSON_SET(b, '$.name', OLD.name), a = JSON_SET(a, '$.name', NEW.name);
    END IF;
    IF NOT (OLD.email <=> NEW.email) THEN
        SET b = JSON_SET(b, '$.email', OLD.email), a = JSON_SET(a, '$.email', NEW.email);
    END IF;
    IF NOT (OLD.role <=> NEW.role) THEN
        SET b = JSON_SET(b, '$.role', OLD.role), a = JSON_SET(a, '$.role', NEW.role);
    END IF;
    IF NOT (OLD.company_id <=> NEW.company_id) THEN
        SET b = JSON_SET(b, '$.company_id', OLD.company_id), a = JSON_SET(a, '$.company_id', NEW.company_id);
    END IF;
    IF NOT (OLD.is_active <=> NEW.is_active) THEN
        SET b = JSON_SET(b, '$.is_active', OLD.is_active), a = JSON_SET(a, '$.is_active', NEW.is_active);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('users', NEW.id, 'UPDATE', b, a, FALSE);
END$$
DELIMITER ;
