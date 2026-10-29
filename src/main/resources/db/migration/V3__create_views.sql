-- Uma linha por etapa com emissão. calculated_at é a data de São Paulo, o fuso do servidor.
CREATE VIEW vw_stage_emission AS
SELECT b.id AS batch_id,
       b.supplier_id,
       p.id AS product_id,
       p.name AS product_name,
       p.category,
       p.unit,
       b.quantity,
       c.id AS chain_id,
       c.stage_type,
       ce.co2_kg,
       ce.emission_factor,
       ce.calculation_method,
       DATE(ce.created_at) AS calculated_at
FROM batch b
JOIN product p ON p.id = b.product_id
JOIN chain c ON c.batch_id = b.id
JOIN carbon_emission ce ON ce.chain_id = c.id;

CREATE VIEW vw_monthly_emission AS
SELECT supplier_id,
       CAST(DATE_FORMAT(calculated_at, '%Y-%m-01') AS DATE) AS month,
       SUM(co2_kg) AS co2_kg
FROM vw_stage_emission
GROUP BY supplier_id, month;

-- Média ponderada pela quantidade: soma por lote antes de somar por fornecedor e produto.
CREATE VIEW vw_supplier_product_emission AS
SELECT supplier_id,
       product_id,
       category,
       unit,
       COUNT(*) AS batch_count,
       SUM(batch_co2_kg) AS total_co2_kg,
       SUM(quantity) AS total_quantity,
       SUM(batch_co2_kg) / SUM(quantity) AS co2_kg_per_unit
FROM (SELECT batch_id, supplier_id, product_id, category, unit, quantity, SUM(co2_kg) AS batch_co2_kg
      FROM vw_stage_emission
      GROUP BY batch_id, supplier_id, product_id, category, unit, quantity) per_batch
GROUP BY supplier_id, product_id, category, unit;

-- Score = 100 + 10 por certificação ativa − 1 a cada 100 kg de CO2, com piso 0.
CREATE VIEW vw_supplier_sustainability AS
SELECT s.id AS supplier_id,
       COALESCE(c.active_certifications, 0) AS active_certifications,
       COALESCE(e.total_co2_kg, 0) AS total_co2_kg,
       CAST(GREATEST(0, 100 + 10 * COALESCE(c.active_certifications, 0)
                        - ROUND(COALESCE(e.total_co2_kg, 0) / 100, 2)) AS DECIMAL(10,2)) AS sustainability_score
FROM supplier s
LEFT JOIN (SELECT supplier_id, COUNT(*) AS active_certifications
           FROM certification
           WHERE status = 'ACTIVE'
           GROUP BY supplier_id) c ON c.supplier_id = s.id
LEFT JOIN (SELECT supplier_id, SUM(co2_kg) AS total_co2_kg
           FROM vw_stage_emission
           GROUP BY supplier_id) e ON e.supplier_id = s.id;

-- Etapa mais recente de cada lote.
CREATE VIEW vw_batch_current_stage AS
SELECT batch_id, chain_id, stage_type, started_at
FROM (SELECT batch_id,
             id AS chain_id,
             stage_type,
             started_at,
             ROW_NUMBER() OVER (PARTITION BY batch_id ORDER BY started_at DESC, id DESC) AS position
      FROM chain) ranked
WHERE position = 1;
