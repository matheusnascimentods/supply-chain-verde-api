-- Retratos gravados na geração, ordenados do mais recente.
SELECT r.id, s.name, r.period_start_at, r.period_end_at,
       r.total_co2_kg, r.batch_count, r.tracked_product_count
FROM report r
JOIN company s ON s.id = r.supplier_id
ORDER BY r.created_at DESC
LIMIT 10;
