-- company repository query: findIdsRankedByScore.
SELECT s.id
FROM company s
JOIN vw_supplier_sustainability v ON v.supplier_id = s.id
WHERE s.type = 'SUPPLIER'
  AND ((:booleanSearch = '' AND :cnpjSearch = '')
   OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR MATCH(s.cnpj) AGAINST (:cnpjSearch IN BOOLEAN MODE))
  AND (:workedWithOnly = FALSE OR EXISTS (
      SELECT 1 FROM batch b WHERE b.supplier_id = s.id AND b.market_id = :marketId))
ORDER BY v.sustainability_score DESC, s.id
LIMIT :limit OFFSET :offset;
