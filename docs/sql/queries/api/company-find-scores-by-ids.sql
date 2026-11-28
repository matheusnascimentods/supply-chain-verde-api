-- company repository query: findScoresByIds.
SELECT supplier_id AS supplierId,
       sustainability_score AS sustainabilityScore,
       active_certifications AS activeCertificationCount,
       total_co2_kg AS totalCo2Kg
FROM vw_supplier_sustainability
WHERE supplier_id IN (:supplierIds);
