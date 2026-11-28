DELIMITER $$
-- audit_report_update, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_report_update
AFTER UPDATE ON report FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.supplier_id <=> NEW.supplier_id) THEN
        SET b = JSON_SET(b, '$.supplier_id', OLD.supplier_id), a = JSON_SET(a, '$.supplier_id', NEW.supplier_id);
    END IF;
    IF NOT (OLD.market_id <=> NEW.market_id) THEN
        SET b = JSON_SET(b, '$.market_id', OLD.market_id), a = JSON_SET(a, '$.market_id', NEW.market_id);
    END IF;
    IF NOT (OLD.period_start_at <=> NEW.period_start_at) THEN
        SET b = JSON_SET(b, '$.period_start_at', OLD.period_start_at), a = JSON_SET(a, '$.period_start_at', NEW.period_start_at);
    END IF;
    IF NOT (OLD.period_end_at <=> NEW.period_end_at) THEN
        SET b = JSON_SET(b, '$.period_end_at', OLD.period_end_at), a = JSON_SET(a, '$.period_end_at', NEW.period_end_at);
    END IF;
    IF NOT (OLD.total_co2_kg <=> NEW.total_co2_kg) THEN
        SET b = JSON_SET(b, '$.total_co2_kg', OLD.total_co2_kg), a = JSON_SET(a, '$.total_co2_kg', NEW.total_co2_kg);
    END IF;
    IF NOT (OLD.tracked_product_count <=> NEW.tracked_product_count) THEN
        SET b = JSON_SET(b, '$.tracked_product_count', OLD.tracked_product_count), a = JSON_SET(a, '$.tracked_product_count', NEW.tracked_product_count);
    END IF;
    IF NOT (OLD.batch_count <=> NEW.batch_count) THEN
        SET b = JSON_SET(b, '$.batch_count', OLD.batch_count), a = JSON_SET(a, '$.batch_count', NEW.batch_count);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('report', NEW.id, 'UPDATE', b, a, FALSE);
END$$
DELIMITER ;
