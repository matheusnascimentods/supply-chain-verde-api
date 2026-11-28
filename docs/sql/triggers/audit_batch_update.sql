DELIMITER $$
-- audit_batch_update, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_batch_update
AFTER UPDATE ON batch FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.product_id <=> NEW.product_id) THEN
        SET b = JSON_SET(b, '$.product_id', OLD.product_id), a = JSON_SET(a, '$.product_id', NEW.product_id);
    END IF;
    IF NOT (OLD.supplier_id <=> NEW.supplier_id) THEN
        SET b = JSON_SET(b, '$.supplier_id', OLD.supplier_id), a = JSON_SET(a, '$.supplier_id', NEW.supplier_id);
    END IF;
    IF NOT (OLD.market_id <=> NEW.market_id) THEN
        SET b = JSON_SET(b, '$.market_id', OLD.market_id), a = JSON_SET(a, '$.market_id', NEW.market_id);
    END IF;
    IF NOT (OLD.status <=> NEW.status) THEN
        SET b = JSON_SET(b, '$.status', OLD.status), a = JSON_SET(a, '$.status', NEW.status);
    END IF;
    IF NOT (OLD.rejection_reason <=> NEW.rejection_reason) THEN
        SET b = JSON_SET(b, '$.rejection_reason', OLD.rejection_reason), a = JSON_SET(a, '$.rejection_reason', NEW.rejection_reason);
    END IF;
    IF NOT (OLD.quantity <=> NEW.quantity) THEN
        SET b = JSON_SET(b, '$.quantity', OLD.quantity), a = JSON_SET(a, '$.quantity', NEW.quantity);
    END IF;
    IF NOT (OLD.produced_at <=> NEW.produced_at) THEN
        SET b = JSON_SET(b, '$.produced_at', OLD.produced_at), a = JSON_SET(a, '$.produced_at', NEW.produced_at);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('batch', NEW.id, 'UPDATE', b, a, NOT (OLD.status <=> NEW.status));
END$$
DELIMITER ;
