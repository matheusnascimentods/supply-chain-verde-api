-- Evolução mensal das emissões da cadeia inteira.
SELECT month, SUM(co2_kg) AS co2_kg
FROM vw_monthly_emission
GROUP BY month
ORDER BY month;
