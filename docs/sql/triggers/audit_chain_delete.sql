DELIMITER $$
-- audit_chain_delete, from V21__enderecos_etapas.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_chain_delete
AFTER DELETE ON chain FOR EACH ROW
CALL audit_write('chain', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'batch_id', OLD.batch_id,
        'destination_address_id', OLD.destination_address_id,
        'responsible_user_id', OLD.responsible_user_id,
        'stage_type', OLD.stage_type,
        'started_at', audit_ts(OLD.started_at),
        'ended_at', audit_ts(OLD.ended_at),
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at),
        'previous_stage_type', OLD.previous_stage_type), NULL, FALSE)$$
DELIMITER ;
