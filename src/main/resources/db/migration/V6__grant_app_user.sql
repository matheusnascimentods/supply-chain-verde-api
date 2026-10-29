-- Grants por tabela: o MySQL não tem privilégio subtrativo, e um GRANT ALL no banco liberaria audit_log (RN-06).
-- O usuário vem do placeholder Flyway app_user e é criado fora das migrations (docker/mysql/init).
GRANT SELECT, INSERT, UPDATE, DELETE ON address TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON batch TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON carbon_emission TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON certification TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON chain TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON product TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON report TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON supplier TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON transport TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON users TO '${app_user}'@'%';

GRANT SELECT ON audit_log TO '${app_user}'@'%';

GRANT SELECT ON vw_stage_emission TO '${app_user}'@'%';
GRANT SELECT ON vw_monthly_emission TO '${app_user}'@'%';
GRANT SELECT ON vw_supplier_product_emission TO '${app_user}'@'%';
GRANT SELECT ON vw_supplier_sustainability TO '${app_user}'@'%';
GRANT SELECT ON vw_batch_current_stage TO '${app_user}'@'%';

GRANT EXECUTE ON PROCEDURE sp_generate_sustainability_report TO '${app_user}'@'%';
GRANT EXECUTE ON PROCEDURE sp_expire_certifications TO '${app_user}'@'%';
