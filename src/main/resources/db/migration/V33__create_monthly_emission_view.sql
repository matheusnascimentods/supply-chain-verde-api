CREATE OR REPLACE VIEW vw_monthly_emission AS
SELECT supplier_id,
       CAST(date_trunc('month', calculated_at) AS DATE) AS month,
       SUM(co2_kg)                                      AS co2_kg
FROM vw_stage_emission
GROUP BY supplier_id, CAST(date_trunc('month', calculated_at) AS DATE);