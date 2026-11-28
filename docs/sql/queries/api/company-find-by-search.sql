-- company repository query: findBySearch.
SELECT s.*
FROM company s
WHERE s.type = 'SUPPLIER'
  AND ((:booleanSearch = '' AND :cnpjSearch = '')
   OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR MATCH(s.cnpj) AGAINST (:cnpjSearch IN BOOLEAN MODE));
