DELIMITER $$
-- audit_product_insert, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_product_insert
AFTER INSERT ON product FOR EACH ROW
CALL audit_write('product', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'supplier_id', NEW.supplier_id,
        'name', NEW.name,
        'category', NEW.category,
        'unit', NEW.unit,
        'description', NEW.description,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$
DELIMITER ;
