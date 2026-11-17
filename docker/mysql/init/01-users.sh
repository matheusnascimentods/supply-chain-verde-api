#!/bin/bash
# Roda uma vez, na criação do volume, como root. As senhas vêm do ambiente, nunca de uma migration.
# O MYSQL_USER da imagem não serve: ele recebe ALL no banco inteiro, inclusive em audit_log.
# Sem set -u: se o arquivo não for executável, a imagem faz source dele no próprio entrypoint.
# A senha entra numa string SQL: use só letras e números (ex.: openssl rand -hex 24).

# supply_audit: DEFINER das triggers de auditoria; ninguém faz login com ela.
# supply_app: datasource da API. Os grants por tabela ficam na V6.
mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE USER 'supply_audit'@'%' IDENTIFIED BY RANDOM PASSWORD ACCOUNT LOCK;
CREATE USER 'supply_app'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD:?defina MYSQL_APP_PASSWORD}';
SQL
