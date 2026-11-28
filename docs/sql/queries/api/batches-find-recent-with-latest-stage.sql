-- batches repository query: findRecentWithLatestStage.
SELECT b.id AS batchId,
       p.name AS productName,
       s.name AS supplierName,
       b.quantity,
       p.unit,
       latest.stage_type AS status
FROM batch b
JOIN product p ON p.id = b.product_id
JOIN company s ON s.id = b.supplier_id
JOIN vw_batch_current_stage latest ON latest.batch_id = b.id
WHERE (:marketId IS NULL OR b.market_id = :marketId)
  AND (:supplierId IS NULL OR b.supplier_id = :supplierId)
ORDER BY b.produced_at DESC, b.id DESC;
