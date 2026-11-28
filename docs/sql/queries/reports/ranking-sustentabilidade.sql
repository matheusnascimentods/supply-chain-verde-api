-- Ranking na mesma ordem de GET /suppliers?ranked=true.
SELECT c.name, v.sustainability_score, v.active_certifications, v.total_co2_kg
FROM vw_supplier_sustainability v
JOIN company c ON c.id = v.supplier_id
ORDER BY v.sustainability_score DESC, v.supplier_id
LIMIT 10;
