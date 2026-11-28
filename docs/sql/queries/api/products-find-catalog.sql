-- products repository query: findCatalog.
SELECT p.id AS productId, p.supplier_id AS supplierId, p.name AS name, p.category AS category,
       p.unit AS unit, p.description AS description, c.name AS supplierName,
       COALESCE(recent.n, 0) AS acceptedBatchesLast30Days,
       co2.co2_kg_per_unit AS co2KgPerUnit
FROM product p
JOIN company c ON c.id = p.supplier_id
LEFT JOIN (SELECT product_id, COUNT(*) AS n
           FROM batch
           WHERE (:marketId IS NULL OR market_id = :marketId)
             AND status IN ('ACTIVE', 'COMPLETED')
             AND created_at >= NOW() - INTERVAL 30 DAY
           GROUP BY product_id) recent ON recent.product_id = p.id
LEFT JOIN (SELECT supplier_id, category, unit,
                  ROUND(SUM(total_co2_kg) / NULLIF(SUM(total_quantity), 0), 4) AS co2_kg_per_unit
           FROM vw_supplier_product_emission
           GROUP BY supplier_id, category, unit) co2
       ON co2.supplier_id = p.supplier_id AND co2.category = p.category AND co2.unit = p.unit
WHERE (:supplierId IS NULL OR p.supplier_id = :supplierId)
  AND (:category IS NULL OR p.category = :category)
  AND (:unit IS NULL OR p.unit = :unit)
  AND ((:booleanSearch = '' AND :categoryCode = '')
   OR MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR MATCH(c.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR (:categoryCode <> '' AND p.category = :categoryCode))
ORDER BY acceptedBatchesLast30Days DESC, co2KgPerUnit IS NULL, co2KgPerUnit, p.name, p.id
LIMIT :limit OFFSET :offset;
