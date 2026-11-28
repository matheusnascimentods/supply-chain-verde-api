DELIMITER $$
-- audit_chain_update, from V21__enderecos_etapas.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_chain_update
AFTER UPDATE ON chain FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.batch_id <=> NEW.batch_id) THEN
        SET b = JSON_SET(b, '$.batch_id', OLD.batch_id), a = JSON_SET(a, '$.batch_id', NEW.batch_id);
    END IF;
    IF NOT (OLD.destination_address_id <=> NEW.destination_address_id) THEN
        SET b = JSON_SET(b, '$.destination_address_id', OLD.destination_address_id), a = JSON_SET(a, '$.destination_address_id', NEW.destination_address_id);
    END IF;
    IF NOT (OLD.responsible_user_id <=> NEW.responsible_user_id) THEN
        SET b = JSON_SET(b, '$.responsible_user_id', OLD.responsible_user_id), a = JSON_SET(a, '$.responsible_user_id', NEW.responsible_user_id);
    END IF;
    IF NOT (OLD.stage_type <=> NEW.stage_type) THEN
        SET b = JSON_SET(b, '$.stage_type', OLD.stage_type), a = JSON_SET(a, '$.stage_type', NEW.stage_type);
    END IF;
    IF NOT (OLD.started_at <=> NEW.started_at) THEN
        SET b = JSON_SET(b, '$.started_at', audit_ts(OLD.started_at)), a = JSON_SET(a, '$.started_at', audit_ts(NEW.started_at));
    END IF;
    IF NOT (OLD.ended_at <=> NEW.ended_at) THEN
        SET b = JSON_SET(b, '$.ended_at', audit_ts(OLD.ended_at)), a = JSON_SET(a, '$.ended_at', audit_ts(NEW.ended_at));
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    IF NOT (OLD.previous_stage_type <=> NEW.previous_stage_type) THEN
        SET b = JSON_SET(b, '$.previous_stage_type', OLD.previous_stage_type), a = JSON_SET(a, '$.previous_stage_type', NEW.previous_stage_type);
    END IF;
    CALL audit_write('chain', NEW.id, 'UPDATE', b, a, NOT (OLD.stage_type <=> NEW.stage_type));
END$$
DELIMITER ;
