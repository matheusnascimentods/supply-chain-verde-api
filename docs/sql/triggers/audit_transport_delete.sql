DELIMITER $$
-- audit_transport_delete, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_transport_delete
AFTER DELETE ON transport FOR EACH ROW
CALL audit_write('transport', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'chain_id', OLD.chain_id,
        'transport_mode', OLD.transport_mode,
        'distance', OLD.distance,
        'fuel_type', OLD.fuel_type,
        'capacity', OLD.capacity,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$
DELIMITER ;
