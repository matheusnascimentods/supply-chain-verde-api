-- RN-14 a RN-16 (019): o score penaliza kg CO2e por tonelada entregue, não o CO2 total.
-- A intensidade só conta lotes com o peso da emissão do TRANSPORT; sem nenhum, ela é 0.
-- ponytail: K = 10. No seed (fator legado), a intensidade vai de 0 a 90 kg/t e o score fica entre 100 e 120;
-- um lote com o cálculo por atividade dá ~435 kg/t. Recalibrar se a intensidade real fugir de 0–1500 kg/t.
CREATE OR REPLACE VIEW vw_supplier_sustainability AS
SELECT s.id AS supplier_id,
       COALESCE(c.active_certifications, 0) AS active_certifications,
       COALESCE(e.total_co2_kg, 0) AS total_co2_kg,
       CAST(GREATEST(0, 100 + 10 * COALESCE(c.active_certifications, 0)
                        - ROUND(COALESCE(i.co2_kg_per_ton, 0) / 10, 2)) AS DECIMAL(10,2)) AS sustainability_score
FROM company s
LEFT JOIN (SELECT supplier_id, COUNT(*) AS active_certifications
           FROM certification
           WHERE status = 'ACTIVE'
           GROUP BY supplier_id) c ON c.supplier_id = s.id
LEFT JOIN (SELECT supplier_id, SUM(co2_kg) AS total_co2_kg
           FROM vw_stage_emission
           GROUP BY supplier_id) e ON e.supplier_id = s.id
LEFT JOIN (SELECT per_batch.supplier_id, SUM(per_batch.co2_kg) / SUM(w.cargo_weight_kg / 1000) AS co2_kg_per_ton
           FROM (SELECT batch_id, supplier_id, SUM(co2_kg) AS co2_kg
                 FROM vw_stage_emission
                 GROUP BY batch_id, supplier_id) per_batch
           JOIN (SELECT ch.batch_id, ce.cargo_weight_kg
                 FROM chain ch
                 JOIN carbon_emission ce ON ce.chain_id = ch.id
                 WHERE ch.stage_type = 'TRANSPORT' AND ce.cargo_weight_kg > 0) w ON w.batch_id = per_batch.batch_id
           GROUP BY per_batch.supplier_id) i ON i.supplier_id = s.id
WHERE s.type = 'SUPPLIER';
