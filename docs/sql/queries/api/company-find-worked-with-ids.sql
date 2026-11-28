-- company repository query: findWorkedWithIds.
SELECT DISTINCT b.supplier_id
FROM batch b
WHERE b.market_id = :marketId AND b.supplier_id IN (:supplierIds);
