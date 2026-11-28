DELIMITER $$
-- audit_batch_insert, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_batch_insert
AFTER INSERT ON batch FOR EACH ROW
CALL audit_write('batch', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'product_id', NEW.product_id,
        'supplier_id', NEW.supplier_id,
        'market_id', NEW.market_id,
        'status', NEW.status,
        'rejection_reason', NEW.rejection_reason,
        'quantity', NEW.quantity,
        'produced_at', NEW.produced_at,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$
DELIMITER ;
