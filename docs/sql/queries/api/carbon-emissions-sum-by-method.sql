-- carbon-emissions repository query: sumByMethod.
SELECT calculation_method AS calculationMethod, emission_factor AS emissionFactor,
       SUM(co2_kg) AS co2Kg
FROM vw_stage_emission
WHERE supplier_id = :supplierId AND market_id = :marketId AND calculated_at BETWEEN :start AND :end
GROUP BY calculation_method, emission_factor
ORDER BY 1, 2;
