DELIMITER $$
-- audit_users_delete, from V17__users_is_active.sql.
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
