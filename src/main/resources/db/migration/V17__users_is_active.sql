-- Feature 016 (RN-05 a RN-14): usuário inativo não loga, não usa a API e não ocupa assento. Todos começam ativos.
ALTER TABLE users
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE AFTER company_id,
    ADD INDEX idx_users_company_active (company_id, is_active);

-- RNF-01: as triggers de auditoria de users passam a registrar is_active.
DROP TRIGGER audit_users_insert;
DROP TRIGGER audit_users_update;
DROP TRIGGER audit_users_delete;

DELIMITER $$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_users_insert
AFTER INSERT ON users FOR EACH ROW
CALL audit_write('users', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'name', NEW.name,
        'email', NEW.email,
        'role', NEW.role,
        'company_id', NEW.company_id,
        'is_active', NEW.is_active,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

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

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_users_delete
AFTER DELETE ON users FOR EACH ROW
CALL audit_write('users', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'name', OLD.name,
        'email', OLD.email,
        'role', OLD.role,
        'company_id', OLD.company_id,
        'is_active', OLD.is_active,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

DELIMITER ;
