DELIMITER $$
-- audit_address_delete, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_address_delete
AFTER DELETE ON address FOR EACH ROW
CALL audit_write('address', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'street', OLD.street,
        'number', OLD.number,
        'neighborhood', OLD.neighborhood,
        'complement', OLD.complement,
        'zip_code', OLD.zip_code,
        'city', OLD.city,
        'state', OLD.state,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$
DELIMITER ;
