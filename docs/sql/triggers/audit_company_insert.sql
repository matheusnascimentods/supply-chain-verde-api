DELIMITER $$
-- audit_company_insert, from V16__company_max_users.sql.
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
DELIMITER ;
