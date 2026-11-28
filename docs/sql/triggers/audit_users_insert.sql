DELIMITER $$
-- audit_users_insert, from V17__users_is_active.sql.
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
DELIMITER ;
