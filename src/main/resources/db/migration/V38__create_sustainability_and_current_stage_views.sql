-- Score = 100 + 10 × certificações ACTIVE − CO₂ total ÷ 100, nunca abaixo de 0.
CREATE VIEW vw_supplier_sustainability AS
SELECT s.id                                 AS supplier_id,
       COALESCE(c.active_certifications, 0) AS active_certifications,
       COALESCE(e.total_co2_kg, 0)          AS total_co2_kg,
       GREATEST(0, 100 + 10 * COALESCE(c.active_certifications, 0)
                       - ROUND(COALESCE(e.total_co2_kg, 0) / 100, 2))::NUMERIC(10, 2) AS sustainability_score
FROM supplier s
LEFT JOIN (SELECT supplier_id, COUNT(*) AS active_certifications
           FROM certification WHERE status = 'ACTIVE' GROUP BY supplier_id) c ON c.supplier_id = s.id
LEFT JOIN (SELECT supplier_id, SUM(co2_kg) AS total_co2_kg
           FROM vw_stage_emission GROUP BY supplier_id) e ON e.supplier_id = s.id;

-- Etapa mais recente de cada lote; desempate por id.
CREATE VIEW vw_batch_current_stage AS
SELECT DISTINCT ON (c.batch_id)
       c.batch_id, c.id AS chain_id, c.stage_type, c.started_at
FROM chain c
ORDER BY c.batch_id, c.started_at DESC, c.id DESC;
