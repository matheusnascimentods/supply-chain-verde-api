-- Feature 016 (RN-01): o teto de usuários passa a ser da companhia, e não do plano. Todas começam com 10. Quem já
-- tem mais ativos que isso fica acima, sem corte (RN-03). plan e company_plan saem na V18.
ALTER TABLE company
    ADD COLUMN max_users INT NOT NULL DEFAULT 10 AFTER type,
    ADD CONSTRAINT chk_company_max_users_positive CHECK (max_users > 0);

-- RNF-01: as triggers de auditoria de company passam a registrar max_users.
DROP TRIGGER audit_company_insert;
DROP TRIGGER audit_company_update;
DROP TRIGGER audit_company_delete;

DELIMITER $$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_insert
AFTER INSERT ON company FOR EACH ROW
CALL audit_write('company', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'name', NEW.name,
        'cnpj', NEW.cnpj,
        'address_id', NEW.address_id,
        'phone', NEW.phone,
        'email', NEW.email,
        'type', NEW.type,
        'max_users', NEW.max_users,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

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

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_delete
AFTER DELETE ON company FOR EACH ROW
CALL audit_write('company', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'name', OLD.name,
        'cnpj', OLD.cnpj,
        'address_id', OLD.address_id,
        'phone', OLD.phone,
        'email', OLD.email,
        'type', OLD.type,
        'max_users', OLD.max_users,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

DELIMITER ;
