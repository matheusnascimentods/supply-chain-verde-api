DELIMITER $$
-- audit_invite_delete, from V12__create_invite.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_invite_delete
AFTER DELETE ON invite FOR EACH ROW
CALL audit_write('invite', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'invited_by', OLD.invited_by,
        'invited_for', OLD.invited_for,
        'role', OLD.role,
        'accepted', OLD.accepted,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$
DELIMITER ;
