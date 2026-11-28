DELIMITER $$
-- audit_company_delete, from V16__company_max_users.sql.
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
