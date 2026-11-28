-- company repository query: countBySearch.
SELECT COUNT(*)
FROM company s
WHERE s.type = 'SUPPLIER'
  AND ((:booleanSearch = '' AND :cnpjSearch = '')
   OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR MATCH(s.cnpj) AGAINST (:cnpjSearch IN BOOLEAN MODE))
  AND (:workedWithOnly = FALSE OR EXISTS (
      SELECT 1 FROM batch b WHERE b.supplier_id = s.id AND b.market_id = :marketId));
