-- products repository query: countBySearch.
SELECT COUNT(*)
FROM product p
WHERE (:supplierId IS NULL OR p.supplier_id = :supplierId)
  AND (MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR (:categoryCode <> '' AND p.category = :categoryCode));
