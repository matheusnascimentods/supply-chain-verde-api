-- carbon-emissions repository query: sumByBatch.
SELECT batch_id AS batchId, product_name AS productName, quantity, unit, SUM(co2_kg) AS co2Kg
FROM vw_stage_emission
WHERE supplier_id = :supplierId AND market_id = :marketId AND calculated_at BETWEEN :start AND :end
GROUP BY batch_id, product_name, quantity, unit
ORDER BY batch_id;
