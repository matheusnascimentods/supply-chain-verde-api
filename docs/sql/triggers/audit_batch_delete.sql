DELIMITER $$
-- audit_batch_delete, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_batch_delete
AFTER DELETE ON batch FOR EACH ROW
CALL audit_write('batch', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'product_id', OLD.product_id,
        'supplier_id', OLD.supplier_id,
        'market_id', OLD.market_id,
        'status', OLD.status,
        'rejection_reason', OLD.rejection_reason,
        'quantity', OLD.quantity,
        'produced_at', OLD.produced_at,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$
DELIMITER ;
