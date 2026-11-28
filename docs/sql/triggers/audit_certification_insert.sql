DELIMITER $$
-- audit_certification_insert, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_certification_insert
AFTER INSERT ON certification FOR EACH ROW
CALL audit_write('certification', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'supplier_id', NEW.supplier_id,
        'certification', NEW.certification,
        'issuing_body', NEW.issuing_body,
        'issued_at', NEW.issued_at,
        'expires_at', NEW.expires_at,
        'status', NEW.status,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$
DELIMITER ;
