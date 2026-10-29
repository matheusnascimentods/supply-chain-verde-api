-- Roda uma vez, na criação do volume, como root. As senhas ficam aqui e no compose, nunca numa migration.
-- O MYSQL_USER da imagem não serve: ele recebe ALL no banco inteiro, inclusive em audit_log.

-- DEFINER das triggers de auditoria; ninguém faz login com ela.
CREATE USER 'supply_audit'@'%' IDENTIFIED BY RANDOM PASSWORD ACCOUNT LOCK;

-- Datasource da API. Os grants por tabela ficam na V6.
CREATE USER 'supply_app'@'%' IDENTIFIED BY 'supply_app';
