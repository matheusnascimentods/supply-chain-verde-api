-- Feature 016 (RN-01): a assinatura é única e o teto mora em company.max_users (V16), então o catálogo de planos e as
-- assinaturas saem. O histórico de company_plan em audit_log fica.

-- O MySQL não apaga o privilégio de tabela junto com ela: sem o REVOKE, uma tabela recriada com o mesmo nome herdaria
-- os grants (padrão da V9 com supplier).
REVOKE SELECT, TRIGGER ON company_plan FROM 'supply_audit'@'%';
REVOKE SELECT, INSERT, UPDATE, DELETE ON company_plan FROM '${app_user}'@'%';
REVOKE SELECT ON plan FROM '${app_user}'@'%';

-- O DROP leva junto as triggers audit_company_plan_*; company_plan sai antes de plan por causa da FK.
DROP TABLE company_plan;
DROP TABLE plan;
