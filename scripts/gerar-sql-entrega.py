"""Gera os scripts SQL da entrega da A3: supply_chain_verde.sql (estrutura, sem dados) e grants.sql.

Uso, com o MySQL do docker compose no ar e migrado: python3 scripts/gerar-sql-entrega.py [pasta-de-saida]
Tabelas, rotinas e triggers vêm do banco; views e event vêm da última migration que os definiu
(o MySQL guarda a view normalizada numa linha só e o STARTS do event como data fixa).
"""
import re
import subprocess
import sys
from datetime import date
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
OUT.mkdir(parents=True, exist_ok=True)
DB = "supply_chain_verde"
SENTINEL = "-- @@FIM@@"


def mysql(sql):
    cmd = ["docker", "compose", "exec", "-T", "mysql", "sh", "-c",
           'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 --raw -N -B ' + DB + ' 2>/dev/null']
    return subprocess.run(cmd, cwd=REPO, input=sql, capture_output=True, text=True, check=True).stdout


def rows(sql):
    """Uma coluna por linha, separada por SENTINEL (os valores podem ter quebra de linha)."""
    out = mysql(sql)
    return [r.strip("\n") for r in out.split(SENTINEL + "\n") if r.strip()]


def show_create(kind, name, col):
    out = mysql(f"SHOW CREATE {kind} `{name}`;")
    return out.rstrip("\n").split("\t")[col]


def section(title):
    bar = "-- " + "=" * 76
    return f"\n{bar}\n-- {title}\n{bar}\n"


# ---------------------------------------------------------------- tabelas, em ordem de dependência das FKs
tables = rows(f"SELECT CONCAT(table_name, '{SENTINEL}') FROM information_schema.tables "
              f"WHERE table_schema = '{DB}' AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history' "
              f"ORDER BY table_name;")
deps = {t: set() for t in tables}
for r in rows(f"SELECT CONCAT(table_name, ' ', referenced_table_name, '{SENTINEL}') "
              f"FROM information_schema.referential_constraints WHERE constraint_schema = '{DB}';"):
    child, parent = r.split()
    if child != parent:
        deps[child].add(parent)
ordered = []
while deps:
    ready = sorted(t for t, d in deps.items() if not d - set(ordered))
    if not ready:
        sys.exit(f"ciclo de FK entre: {sorted(deps)}")
    for t in ready:
        ordered.append(t)
        del deps[t]

# ---------------------------------------------------------------- views e event: última definição nas migrations
def version(p):
    return int(re.match(r"V(\d+)__", p.name).group(1))


migrations = sorted((REPO / "src/main/resources/db/migration").glob("V*__*.sql"), key=version)
views, event = {}, None
for m in migrations:
    text = m.read_text()
    for v in re.finditer(r"^CREATE (?:OR REPLACE )?VIEW (\w+) AS\n(.*?;)$", text, re.M | re.S):
        views[v.group(1)] = f"CREATE VIEW {v.group(1)} AS\n{v.group(2)}"
    e = re.search(r"^CREATE EVENT .*?;$", text, re.M | re.S)
    if e:
        event = e.group(0)
db_views = rows(f"SELECT CONCAT(table_name, '{SENTINEL}') FROM information_schema.views WHERE table_schema = '{DB}';")
assert sorted(views) == sorted(db_views), (sorted(views), sorted(db_views))
view_order, pending = [], dict(views)
while pending:
    ready = sorted(n for n, body in pending.items()
                   if not any(o in body for o in pending if o != n))
    for n in ready:
        view_order.append(n)
        del pending[n]

# ---------------------------------------------------------------- rotinas e triggers, do banco
routines = [r.split(" ") for r in rows(
    f"SELECT CONCAT(routine_type, ' ', routine_name, '{SENTINEL}') FROM information_schema.routines "
    f"WHERE routine_schema = '{DB}' ORDER BY routine_type, routine_name;")]
triggers = [r.split(" ") for r in rows(
    f"SELECT CONCAT(event_object_table, ' ', trigger_name, '{SENTINEL}') FROM information_schema.triggers "
    f"WHERE trigger_schema = '{DB}' ORDER BY event_object_table, action_timing, "
    f"FIELD(event_manipulation, 'INSERT', 'UPDATE', 'DELETE');")]
sql_mode = rows(f"SELECT CONCAT(sql_mode, '{SENTINEL}') FROM information_schema.routines "
                f"WHERE routine_schema = '{DB}' LIMIT 1;")[0]
modes = set(rows(f"SELECT DISTINCT CONCAT(sql_mode, '{SENTINEL}') FROM information_schema.triggers WHERE trigger_schema = '{DB}' "
                 f"UNION SELECT DISTINCT CONCAT(sql_mode, '{SENTINEL}') FROM information_schema.routines WHERE routine_schema = '{DB}';"))
assert modes == {sql_mode}, modes  # um SET sql_mode só no cabeçalho vale para todos


def without_root_definer(stmt):
    """DEFINER root é o padrão de quem restaura; só o de supply_audit (auditoria) é relevante."""
    return stmt.replace("DEFINER=`root`@`%` ", "")


# ---------------------------------------------------------------- supply_chain_verde.sql
o = [f"""-- Supply Chain Verde: estrutura do banco (MySQL 8.4)
-- Gerado em {date.today():%d/%m/%Y} a partir do banco migrado pelo Flyway. Não contém dados.
--
-- Restauração, como root:
--   1. Criar as contas de serviço (docker/mysql/init/01-users.sh): supply_audit é o DEFINER
--      das triggers de auditoria e precisa existir antes da primeira escrita.
--   2. Executar este arquivo.
--   3. Executar grants.sql.
--
-- Seções: tabelas, funções e procedures, views, triggers de auditoria e event.

SET NAMES utf8mb4;
SET sql_mode = '{sql_mode}';
"""]
o.append(section("Banco de dados"))
o.append(f"\nCREATE DATABASE IF NOT EXISTS `{DB}`\n  DEFAULT CHARACTER SET utf8mb4\n  COLLATE utf8mb4_0900_as_cs;\n\nUSE `{DB}`;\n")

o.append(section(f"Tabelas ({len(ordered)}), em ordem de dependência das chaves estrangeiras"))
for t in ordered:
    ddl = re.sub(r" AUTO_INCREMENT=\d+", "", show_create("TABLE", t, 1))
    o.append(f"\n-- {t}\n{ddl};\n")

o.append(section(f"Funções e procedures ({len(routines)})"))
o.append("\nDELIMITER $$\n")
for kind, name in routines:
    o.append(f"\n-- {name}\n{without_root_definer(show_create(kind, name, 2))}$$\n")
o.append("\nDELIMITER ;\n")

o.append(section(f"Views ({len(view_order)}), cada uma depois das que ela usa"))
for n in view_order:
    o.append(f"\n-- {n}\n{views[n]}\n")

o.append(section(f"Triggers de auditoria ({len(triggers)}): uma por tabela e operação, com DEFINER supply_audit"))
o.append("\nDELIMITER $$\n")
current = None
for table, name in triggers:
    if table != current:
        o.append(f"\n-- ---------------------------------------------------------------- {table}\n")
        current = table
    o.append(f"\n{show_create('TRIGGER', name, 2)}$$\n")
o.append("\nDELIMITER ;\n")

o.append(section("Event: expira as certificações vencidas todo dia à meia-noite (America/Sao_Paulo)"))
o.append(f"\nSET time_zone = 'America/Sao_Paulo';\n\n{without_root_definer(event)}\n")
(OUT / "supply_chain_verde.sql").write_text("".join(o))

# ---------------------------------------------------------------- grants.sql
def grants(user):
    return [g for g in mysql(f"SHOW GRANTS FOR {user};").splitlines() if not g.startswith("GRANT USAGE")]


def target(g):
    m = re.search(r" ON (?:(PROCEDURE|FUNCTION) )?`[^`]+`\.`([^`]+)` TO", g)
    return (m.group(1) or "TABLE", m.group(2))


g = [f"""-- Supply Chain Verde: privilégios das contas de serviço (MySQL 8.4)
-- Gerado em {date.today():%d/%m/%Y}. Executar como root, depois de supply_chain_verde.sql.
-- As contas são criadas antes, por docker/mysql/init/01-users.sh; este arquivo não tem senhas.
-- Privilégio mínimo: nenhuma conta tem GRANT ALL, e só supply_audit grava em audit_log (ADR 0008).
"""]
view_set = set(views)
for user, title in [("supply_app", "supply_app: conta da API (datasource)"),
                    ("supply_audit", "supply_audit: DEFINER das triggers de auditoria (ACCOUNT LOCK, ninguém faz login)")]:
    g.append(section(title))
    buckets = {"Tabelas": [], "Views": [], "Procedures e funções": []}
    for line in grants(user):
        kind, obj = target(line)
        key = "Procedures e funções" if kind != "TABLE" else ("Views" if obj in view_set else "Tabelas")
        buckets[key].append((obj, line + ";"))
    for key, items in buckets.items():
        if items:
            g.append(f"\n-- {key}\n" + "\n".join(line for _, line in sorted(items)) + "\n")
(OUT / "grants.sql").write_text("".join(g))
print("ok:", len(ordered), "tabelas,", len(routines), "rotinas,", len(view_order), "views,", len(triggers), "triggers")
