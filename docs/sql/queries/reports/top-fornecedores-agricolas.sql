-- Top 5 fornecedores com menor CO2 por kg de produtos agrícolas.
SELECT s.name, v.co2_kg_per_unit
FROM vw_supplier_product_emission v
JOIN company s ON s.id = v.supplier_id
WHERE v.category = 'AGRICULTURE' AND v.unit = 'KG'
ORDER BY v.co2_kg_per_unit
LIMIT 5;
