-- products repository query: countCatalog.
SELECT COUNT(*)
FROM product p
JOIN company c ON c.id = p.supplier_id
WHERE (:supplierId IS NULL OR p.supplier_id = :supplierId)
  AND (:category IS NULL OR p.category = :category)
  AND (:unit IS NULL OR p.unit = :unit)
  AND ((:booleanSearch = '' AND :categoryCode = '')
   OR MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR MATCH(c.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR (:categoryCode <> '' AND p.category = :categoryCode));
