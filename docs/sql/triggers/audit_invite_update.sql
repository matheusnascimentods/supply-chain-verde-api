DELIMITER $$
-- audit_invite_update, from V12__create_invite.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_invite_update
AFTER UPDATE ON invite FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.invited_by <=> NEW.invited_by) THEN
        SET b = JSON_SET(b, '$.invited_by', OLD.invited_by), a = JSON_SET(a, '$.invited_by', NEW.invited_by);
    END IF;
    IF NOT (OLD.invited_for <=> NEW.invited_for) THEN
        SET b = JSON_SET(b, '$.invited_for', OLD.invited_for), a = JSON_SET(a, '$.invited_for', NEW.invited_for);
    END IF;
    IF NOT (OLD.role <=> NEW.role) THEN
        SET b = JSON_SET(b, '$.role', OLD.role), a = JSON_SET(a, '$.role', NEW.role);
    END IF;
    IF NOT (OLD.accepted <=> NEW.accepted) THEN
        SET b = JSON_SET(b, '$.accepted', OLD.accepted), a = JSON_SET(a, '$.accepted', NEW.accepted);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('invite', NEW.id, 'UPDATE', b, a, FALSE);
END$$
DELIMITER ;
