#!/usr/bin/env bash
# Gera os scripts SQL da entrega da A3: supply_chain_verde.sql (estrutura, sem dados) e grants.sql.
#
# Uso, com o MySQL do docker compose no ar e migrado: scripts/gerar-sql-entrega.sh [pasta-de-saida]
# Tabelas, rotinas e triggers vêm do banco. Views e event vêm da última migration que os definiu:
# o MySQL guarda a view normalizada numa linha só e o STARTS do event como data fixa.
set -euo pipefail

mkdir -p "${1:-.}"
OUT=$(cd "${1:-.}" && pwd)  # absoluta antes do cd: a pasta é relativa a quem chamou
cd "$(dirname "$0")/.."
DB=supply_chain_verde
T=$'\t'
MIGRATIONS=src/main/resources/db/migration
BAR="-- $(printf '=%.0s' {1..76})"

# Executa o SQL da entrada padrão no MySQL do compose. --raw: quebras de linha dos corpos saem como estão.
q() {
  docker compose exec -T mysql sh -c \
    "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --default-character-set=utf8mb4 --raw -N -B $DB 2>/dev/null"
}

# Só o comando CREATE do SHOW CREATE. TABLE: tira a 1ª coluna. Rotina e trigger: tira as 2 primeiras
# colunas (na 1ª linha) e as colunas depois do comando (na última linha).
show_create() {
  local out
  out=$(q <<< "SHOW CREATE $1 \`$2\`;")
  if [ "$1" = TABLE ]; then
    printf '%s\n' "$out" | sed "1s/^[^$T]*$T//"
  else
    printf '%s\n' "$out" | sed -e "1s/^[^$T]*$T[^$T]*$T//" -e "\$s/$T.*//"
  fi
}

# DEFINER root é o padrão de quem restaura; só o de supply_audit (auditoria) é relevante.
without_root_definer() { sed 's/DEFINER=`root`@`%` //'; }

section() { printf '\n%s\n-- %s\n%s\n' "$BAR" "$1" "$BAR"; }

# Migrations em ordem de versão (V2 antes de V10).
migrations() {
  for f in "$MIGRATIONS"/V*__*.sql; do n=${f##*/V}; printf '%s\t%s\n' "${n%%__*}" "$f"; done \
    | sort -n | cut -f2-
}

# Última definição nas migrations: view_def <nome> imprime a view; event_def imprime o event.
view_def() {
  migrations | xargs awk -v want="$1" '
    /^CREATE (OR REPLACE )?VIEW [A-Za-z0-9_]+ AS$/ {
      n = $0; sub(/^CREATE (OR REPLACE )?VIEW /, "", n); sub(/ AS$/, "", n)
      cap = 1; buf = "CREATE VIEW " n " AS"; next
    }
    cap { buf = buf "\n" $0; if ($0 ~ /;$/) { def[n] = buf; cap = 0 } }
    END { print def[want] }'
}
event_def() {
  migrations | xargs awk '
    /^CREATE EVENT / { cap = 1; buf = "" }
    cap { buf = (buf == "" ? $0 : buf "\n" $0); if ($0 ~ /;$/) { def = buf; cap = 0 } }
    END { print def }'
}

# ------------------------------------------------------------------ levantamento no banco
# Tabelas: profundidade = maior caminho de FK até ela; ordenar por ela põe toda tabela depois das que referencia.
tables=$(q <<'SQL'
WITH RECURSIVE d AS (
  SELECT table_name AS t, 0 AS depth FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'
  UNION ALL
  SELECT r.table_name, d.depth + 1 FROM information_schema.referential_constraints r
  JOIN d ON r.referenced_table_name = d.t AND r.table_name <> r.referenced_table_name
  WHERE r.constraint_schema = DATABASE()
)
SELECT t FROM d GROUP BY t ORDER BY MAX(depth), t;
SQL
)
# Views: a mesma ideia, pelas views que cada uma usa.
views=$(q <<'SQL'
WITH RECURSIVE d AS (
  SELECT table_name AS v, 0 AS depth FROM information_schema.views WHERE table_schema = DATABASE()
  UNION ALL
  SELECT u.view_name, d.depth + 1 FROM information_schema.view_table_usage u
  JOIN d ON u.table_name = d.v AND u.table_schema = DATABASE()
  WHERE u.view_schema = DATABASE()
)
SELECT v FROM d GROUP BY v ORDER BY MAX(depth), v;
SQL
)
routines=$(q <<< "SELECT routine_type, routine_name FROM information_schema.routines
                  WHERE routine_schema = DATABASE() ORDER BY routine_type, routine_name;")
triggers=$(q <<< "SELECT event_object_table, trigger_name FROM information_schema.triggers
                  WHERE trigger_schema = DATABASE()
                  ORDER BY event_object_table, action_timing, FIELD(event_manipulation, 'INSERT', 'UPDATE', 'DELETE');")
modes=$(q <<< "SELECT DISTINCT sql_mode FROM information_schema.triggers WHERE trigger_schema = DATABASE()
               UNION SELECT DISTINCT sql_mode FROM information_schema.routines WHERE routine_schema = DATABASE();")

# Um SET sql_mode só no cabeçalho vale para todos os objetos.
[ "$(printf '%s\n' "$modes" | wc -l)" -eq 1 ] || { echo "triggers e rotinas com sql_mode diferentes:" >&2; echo "$modes" >&2; exit 1; }
sql_mode=$modes
# Toda view do banco precisa ter definição nas migrations.
for v in $views; do [ -n "$(view_def "$v")" ] || { echo "view $v sem CREATE VIEW nas migrations" >&2; exit 1; }; done

count() { printf '%s\n' "$1" | grep -c .; }

# ------------------------------------------------------------------ supply_chain_verde.sql
{
  cat <<EOF
-- Supply Chain Verde: estrutura do banco (MySQL 8.4)
-- Gerado em $(date +%d/%m/%Y) a partir do banco migrado pelo Flyway. Não contém dados.
--
-- Restauração, como root:
--   1. Criar as contas de serviço (docker/mysql/init/01-users.sh): supply_audit é o DEFINER
--      das triggers de auditoria e precisa existir antes da primeira escrita.
--   2. Executar este arquivo.
--   3. Executar grants.sql.
--
-- Seções: tabelas, funções e procedures, views, triggers de auditoria e event.

SET NAMES utf8mb4;
SET sql_mode = '$sql_mode';
EOF

  section "Banco de dados"
  printf '\nCREATE DATABASE IF NOT EXISTS `%s`\n  DEFAULT CHARACTER SET utf8mb4\n  COLLATE utf8mb4_0900_as_cs;\n\nUSE `%s`;\n' "$DB" "$DB"

  section "Tabelas ($(count "$tables")), em ordem de dependência das chaves estrangeiras"
  for t in $tables; do
    printf '\n-- %s\n%s;\n' "$t" "$(show_create TABLE "$t" | sed 's/ AUTO_INCREMENT=[0-9]*//')"
  done

  section "Funções e procedures ($(count "$routines"))"
  printf '\nDELIMITER $$\n'
  while IFS=$T read -r kind name; do
    printf '\n-- %s\n%s$$\n' "$name" "$(show_create "$kind" "$name" | without_root_definer)"
  done <<< "$routines"
  printf '\nDELIMITER ;\n'

  section "Views ($(count "$views")), cada uma depois das que ela usa"
  for v in $views; do printf '\n-- %s\n%s\n' "$v" "$(view_def "$v")"; done

  section "Triggers de auditoria ($(count "$triggers")): uma por tabela e operação, com DEFINER supply_audit"
  printf '\nDELIMITER $$\n'
  current=
  while IFS=$T read -r table name; do
    if [ "$table" != "$current" ]; then
      printf '\n-- ---------------------------------------------------------------- %s\n' "$table"
      current=$table
    fi
    printf '\n%s$$\n' "$(show_create TRIGGER "$name")"
  done <<< "$triggers"
  printf '\nDELIMITER ;\n'

  section "Event: expira as certificações vencidas todo dia à meia-noite (America/Sao_Paulo)"
  printf "\nSET time_zone = 'America/Sao_Paulo';\n\n%s\n" "$(event_def | without_root_definer)"
} > "$OUT/supply_chain_verde.sql"

# ------------------------------------------------------------------ grants.sql
# Uma linha por privilégio: "grupo<TAB>objeto<TAB>GRANT ...;", ordenada por objeto.
grants() {
  q <<< "SHOW GRANTS FOR $1;" | awk -v views=" $(echo $views) " '
    /^GRANT USAGE/ { next }
    {
      obj = $0; group = "Tabelas"
      if ($0 ~ / ON (PROCEDURE|FUNCTION) /) group = "Procedures e funções"
      sub(/.* ON (PROCEDURE |FUNCTION )?`[^`]+`\.`/, "", obj); sub(/` TO .*/, "", obj)
      if (group == "Tabelas" && index(views, " " obj " ")) group = "Views"
      print group "\t" obj "\t" $0 ";"
    }' | LC_ALL=C sort -t"$T" -k2
}

{
  cat <<EOF
-- Supply Chain Verde: privilégios das contas de serviço (MySQL 8.4)
-- Gerado em $(date +%d/%m/%Y). Executar como root, depois de supply_chain_verde.sql.
-- As contas são criadas antes, por docker/mysql/init/01-users.sh; este arquivo não tem senhas.
-- Privilégio mínimo: nenhuma conta tem GRANT ALL, e só supply_audit grava em audit_log (ADR 0008).
EOF
  for account in "supply_app|supply_app: conta da API (datasource)" \
                 "supply_audit|supply_audit: DEFINER das triggers de auditoria (ACCOUNT LOCK, ninguém faz login)"; do
    user=${account%%|*}
    section "${account#*|}"
    lines=$(grants "$user")
    for group in "Tabelas" "Views" "Procedures e funções"; do
      block=$(printf '%s\n' "$lines" | awk -F"$T" -v g="$group" '$1 == g { print $3 }')
      if [ -n "$block" ]; then printf '\n-- %s\n%s\n' "$group" "$block"; fi
    done
  done
} > "$OUT/grants.sql"

echo "ok: $(count "$tables") tabelas, $(count "$routines") rotinas, $(count "$views") views, $(count "$triggers") triggers em $OUT"
