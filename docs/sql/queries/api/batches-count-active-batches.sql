-- batches repository query: countActiveBatches.
SELECT COUNT(*)
FROM batch b
WHERE b.status = 'ACTIVE'
  AND (:marketId IS NULL OR b.market_id = :marketId)
  AND (:supplierId IS NULL OR b.supplier_id = :supplierId);
