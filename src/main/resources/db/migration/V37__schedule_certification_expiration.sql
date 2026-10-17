-- Requer a imagem docker/postgres (shared_preload_libraries=pg_cron, cron.database_name = este banco).
-- O pg_cron usa UTC: 03:00 UTC é meia-noite em São Paulo.
CREATE EXTENSION IF NOT EXISTS pg_cron;

SELECT cron.schedule('expire-certifications', '0 3 * * *', 'CALL sp_expire_certifications()');
