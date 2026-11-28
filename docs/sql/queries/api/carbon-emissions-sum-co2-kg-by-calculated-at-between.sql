-- carbon-emissions repository query: sumCo2KgByCalculatedAtBetween.
SELECT SUM(co2_kg)
FROM vw_stage_emission
WHERE calculated_at BETWEEN :startsAt AND :endsAt
  AND (:marketId IS NULL OR market_id = :marketId)
  AND (:supplierId IS NULL OR supplier_id = :supplierId);
