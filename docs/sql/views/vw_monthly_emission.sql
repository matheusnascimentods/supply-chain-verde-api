-- Current definition from V9__drop_supplier.sql.
CREATE OR REPLACE VIEW vw_monthly_emission AS
SELECT supplier_id,
       market_id,
       CAST(DATE_FORMAT(calculated_at, '%Y-%m-01') AS DATE) AS month,
       SUM(co2_kg) AS co2_kg
FROM vw_stage_emission
GROUP BY supplier_id, market_id, month;
