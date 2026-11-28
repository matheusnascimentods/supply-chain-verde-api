-- products repository query: findPage.
SELECT id, name, category, unit, description, supplier_id
FROM product
WHERE (:supplierId IS NULL OR supplier_id = :supplierId)
ORDER BY id ASC
LIMIT :limit OFFSET :offset;
