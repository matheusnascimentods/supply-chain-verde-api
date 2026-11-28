DELIMITER $$
-- audit_chain_insert, from V21__enderecos_etapas.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_chain_insert
AFTER INSERT ON chain FOR EACH ROW
CALL audit_write('chain', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'batch_id', NEW.batch_id,
        'destination_address_id', NEW.destination_address_id,
        'responsible_user_id', NEW.responsible_user_id,
        'stage_type', NEW.stage_type,
        'started_at', audit_ts(NEW.started_at),
        'ended_at', audit_ts(NEW.ended_at),
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at),
        'previous_stage_type', NEW.previous_stage_type), FALSE)$$
DELIMITER ;
