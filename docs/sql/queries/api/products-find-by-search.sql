-- products repository query: findBySearch.
SELECT p.id, p.name, p.category, p.unit, p.description, p.supplier_id
FROM product p
WHERE (:supplierId IS NULL OR p.supplier_id = :supplierId)
  AND (MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR (:categoryCode <> '' AND p.category = :categoryCode))
ORDER BY p.id ASC
LIMIT :limit OFFSET :offset;
