-- users repository query: findIdsByEmailSearch.
SELECT id
FROM users
WHERE MATCH(email) AGAINST (:booleanSearch IN BOOLEAN MODE);
