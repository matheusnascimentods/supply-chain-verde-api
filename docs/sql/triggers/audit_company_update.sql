DELIMITER $$
-- audit_company_update, from V16__company_max_users.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_update
AFTER UPDATE ON company FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (BINARY OLD.name <=> BINARY NEW.name) THEN
        SET b = JSON_SET(b, '$.name', OLD.name), a = JSON_SET(a, '$.name', NEW.name);
    END IF;
    IF NOT (OLD.cnpj <=> NEW.cnpj) THEN
        SET b = JSON_SET(b, '$.cnpj', OLD.cnpj), a = JSON_SET(a, '$.cnpj', NEW.cnpj);
    END IF;
    IF NOT (OLD.address_id <=> NEW.address_id) THEN
        SET b = JSON_SET(b, '$.address_id', OLD.address_id), a = JSON_SET(a, '$.address_id', NEW.address_id);
    END IF;
    IF NOT (OLD.phone <=> NEW.phone) THEN
        SET b = JSON_SET(b, '$.phone', OLD.phone), a = JSON_SET(a, '$.phone', NEW.phone);
    END IF;
    IF NOT (OLD.email <=> NEW.email) THEN
        SET b = JSON_SET(b, '$.email', OLD.email), a = JSON_SET(a, '$.email', NEW.email);
    END IF;
    IF NOT (OLD.type <=> NEW.type) THEN
        SET b = JSON_SET(b, '$.type', OLD.type), a = JSON_SET(a, '$.type', NEW.type);
    END IF;
    IF NOT (OLD.max_users <=> NEW.max_users) THEN
        SET b = JSON_SET(b, '$.max_users', OLD.max_users), a = JSON_SET(a, '$.max_users', NEW.max_users);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('company', NEW.id, 'UPDATE', b, a, FALSE);
END$$
DELIMITER ;
