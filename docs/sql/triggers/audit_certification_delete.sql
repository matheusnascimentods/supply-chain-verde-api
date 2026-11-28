DELIMITER $$
-- audit_certification_delete, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_certification_delete
AFTER DELETE ON certification FOR EACH ROW
CALL audit_write('certification', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'supplier_id', OLD.supplier_id,
        'certification', OLD.certification,
        'issuing_body', OLD.issuing_body,
        'issued_at', OLD.issued_at,
        'expires_at', OLD.expires_at,
        'status', OLD.status,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$
DELIMITER ;
