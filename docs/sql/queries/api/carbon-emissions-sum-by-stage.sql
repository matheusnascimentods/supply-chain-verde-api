-- carbon-emissions repository query: sumByStage.
SELECT stage_type AS stageType, SUM(co2_kg) AS co2Kg
FROM vw_stage_emission
WHERE supplier_id = :supplierId AND market_id = :marketId AND calculated_at BETWEEN :start AND :end
GROUP BY stage_type
ORDER BY 2 DESC;
