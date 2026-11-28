DELIMITER $$
-- audit_product_delete, from V9__drop_supplier.sql.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_product_delete
AFTER DELETE ON product FOR EACH ROW
CALL audit_write('product', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'supplier_id', OLD.supplier_id,
        'name', OLD.name,
        'category', OLD.category,
        'unit', OLD.unit,
        'description', OLD.description,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$
DELIMITER ;
