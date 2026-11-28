-- Etapas ordenadas pela emissão total.
SELECT stage_type, SUM(co2_kg) AS co2_kg
FROM vw_stage_emission
GROUP BY stage_type
ORDER BY 2 DESC;
