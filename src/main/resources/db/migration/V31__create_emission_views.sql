CREATE OR REPLACE VIEW vw_stage_emission AS
SELECT b.batch_id,
       b.supplier_id,
       p.product_id,
       p.name        AS product_name,
       p.category,
       p.unit,
       b.quantity,
       c.chain_id,
       c.stage_type,
       ce.co2_kg,
       ce.emission_factor,
       ce.calculation_method,
       ce.calculated_at
FROM batch b
         JOIN product p          ON p.product_id = b.product_id
         JOIN chain c            ON c.batch_id = b.batch_id
         JOIN carbon_emission ce ON ce.chain_id = c.chain_id;

CREATE OR REPLACE VIEW vw_supplier_product_emission AS
SELECT supplier_id,
       product_id,
       category,
       unit,
       COUNT(*)                          AS batch_count,
       SUM(batch_co2_kg)                 AS total_co2_kg,
       SUM(quantity)                     AS total_quantity,
       SUM(batch_co2_kg) / SUM(quantity) AS co2_kg_per_unit
FROM (
         SELECT batch_id, supplier_id, product_id, category, unit, quantity, SUM(co2_kg) AS batch_co2_kg
         FROM vw_stage_emission
         GROUP BY batch_id, supplier_id, product_id, category, unit, quantity
     ) per_batch
GROUP BY supplier_id, product_id, category, unit;