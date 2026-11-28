DELIMITER $$
-- audit_transport_insert, from V5__create_audit.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_transport_insert
AFTER INSERT ON transport FOR EACH ROW
CALL audit_write('transport', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'chain_id', NEW.chain_id,
        'transport_mode', NEW.transport_mode,
        'distance', NEW.distance,
        'fuel_type', NEW.fuel_type,
        'capacity', NEW.capacity,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$
DELIMITER ;
