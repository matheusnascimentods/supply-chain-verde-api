DELIMITER $$
-- audit_certification_update, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_certification_update
AFTER UPDATE ON certification FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.supplier_id <=> NEW.supplier_id) THEN
        SET b = JSON_SET(b, '$.supplier_id', OLD.supplier_id), a = JSON_SET(a, '$.supplier_id', NEW.supplier_id);
    END IF;
    IF NOT (OLD.certification <=> NEW.certification) THEN
        SET b = JSON_SET(b, '$.certification', OLD.certification), a = JSON_SET(a, '$.certification', NEW.certification);
    END IF;
    IF NOT (OLD.issuing_body <=> NEW.issuing_body) THEN
        SET b = JSON_SET(b, '$.issuing_body', OLD.issuing_body), a = JSON_SET(a, '$.issuing_body', NEW.issuing_body);
    END IF;
    IF NOT (OLD.issued_at <=> NEW.issued_at) THEN
        SET b = JSON_SET(b, '$.issued_at', OLD.issued_at), a = JSON_SET(a, '$.issued_at', NEW.issued_at);
    END IF;
    IF NOT (OLD.expires_at <=> NEW.expires_at) THEN
        SET b = JSON_SET(b, '$.expires_at', OLD.expires_at), a = JSON_SET(a, '$.expires_at', NEW.expires_at);
    END IF;
    IF NOT (OLD.status <=> NEW.status) THEN
        SET b = JSON_SET(b, '$.status', OLD.status), a = JSON_SET(a, '$.status', NEW.status);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('certification', NEW.id, 'UPDATE', b, a, NOT (OLD.status <=> NEW.status));
END$$
DELIMITER ;
