DELIMITER $$
-- audit_product_update, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_product_update
AFTER UPDATE ON product FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.supplier_id <=> NEW.supplier_id) THEN
        SET b = JSON_SET(b, '$.supplier_id', OLD.supplier_id), a = JSON_SET(a, '$.supplier_id', NEW.supplier_id);
    END IF;
    IF NOT (BINARY OLD.name <=> BINARY NEW.name) THEN
        SET b = JSON_SET(b, '$.name', OLD.name), a = JSON_SET(a, '$.name', NEW.name);
    END IF;
    IF NOT (OLD.category <=> NEW.category) THEN
        SET b = JSON_SET(b, '$.category', OLD.category), a = JSON_SET(a, '$.category', NEW.category);
    END IF;
    IF NOT (OLD.unit <=> NEW.unit) THEN
        SET b = JSON_SET(b, '$.unit', OLD.unit), a = JSON_SET(a, '$.unit', NEW.unit);
    END IF;
    IF NOT (BINARY OLD.description <=> BINARY NEW.description) THEN
        SET b = JSON_SET(b, '$.description', OLD.description), a = JSON_SET(a, '$.description', NEW.description);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('product', NEW.id, 'UPDATE', b, a, FALSE);
END$$
DELIMITER ;
