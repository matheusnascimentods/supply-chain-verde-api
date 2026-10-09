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