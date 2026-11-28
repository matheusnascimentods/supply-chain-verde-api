DELIMITER $$
-- audit_report_insert, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_report_insert
AFTER INSERT ON report FOR EACH ROW
CALL audit_write('report', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'supplier_id', NEW.supplier_id,
        'market_id', NEW.market_id,
        'period_start_at', NEW.period_start_at,
        'period_end_at', NEW.period_end_at,
        'total_co2_kg', NEW.total_co2_kg,
        'tracked_product_count', NEW.tracked_product_count,
        'batch_count', NEW.batch_count,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$
DELIMITER ;
