DELIMITER $$
-- audit_address_insert, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_address_insert
AFTER INSERT ON address FOR EACH ROW
CALL audit_write('address', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'street', NEW.street,
        'number', NEW.number,
        'neighborhood', NEW.neighborhood,
        'complement', NEW.complement,
        'zip_code', NEW.zip_code,
        'city', NEW.city,
        'state', NEW.state,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$
DELIMITER ;
