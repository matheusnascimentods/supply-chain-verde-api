DELIMITER $$
-- audit_report_delete, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_report_delete
AFTER DELETE ON report FOR EACH ROW
CALL audit_write('report', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'supplier_id', OLD.supplier_id,
        'market_id', OLD.market_id,
        'period_start_at', OLD.period_start_at,
        'period_end_at', OLD.period_end_at,
        'total_co2_kg', OLD.total_co2_kg,
        'tracked_product_count', OLD.tracked_product_count,
        'batch_count', OLD.batch_count,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$
DELIMITER ;
