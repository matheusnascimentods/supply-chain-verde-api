-- carbon-emissions repository query: sumMonthly.
SELECT DATE_FORMAT(month, '%Y-%m') AS month, SUM(co2_kg) AS co2Kg
FROM vw_monthly_emission
WHERE month BETWEEN :from AND :to
  AND (:supplierId IS NULL OR supplier_id = :supplierId)
  AND (:marketId IS NULL OR market_id = :marketId)
GROUP BY month
ORDER BY month;
