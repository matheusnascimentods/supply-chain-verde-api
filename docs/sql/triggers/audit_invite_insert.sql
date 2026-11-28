DELIMITER $$
-- audit_invite_insert, from V12__create_invite.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_invite_insert
AFTER INSERT ON invite FOR EACH ROW
CALL audit_write('invite', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'invited_by', NEW.invited_by,
        'invited_for', NEW.invited_for,
        'role', NEW.role,
        'accepted', NEW.accepted,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$
DELIMITER ;
