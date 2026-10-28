# Plano técnico — Migração de PostgreSQL para MySQL

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Inventário: o que é PostgreSQL hoje e para onde vai

| # | Recurso PG | Onde | MySQL 8.4 | Custo |
|---|---|---|---|---|
| 1 | `CREATE TYPE ... AS ENUM` (9 tipos) + `@JdbcTypeCode(NAMED_ENUM)` | V3–V11, 7 entidades JPA | `ENUM(...)` inline na coluna; nas entidades sai o `@JdbcTypeCode` e fica `@Enumerated(STRING)` | baixo |
| 2 | `BIGSERIAL` | todas as tabelas | `BIGINT AUTO_INCREMENT` | baixo |
| 3 | `TIMESTAMPTZ` / `TIMESTAMP` + `AT TIME ZONE 'America/Sao_Paulo'` | V35, views | `DATETIME(6)` no horário de São Paulo, igual à JVM (`TimeZone.setDefault` em `SupplyChainVerdeApplication`); servidor com `--default-time-zone=America/Sao_Paulo` (§4) | médio |
| 4 | Trigger `set_updated_at` | V35 | `updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)`, nativo; só muda quando a linha muda (RN-08) | **some** |
| 5 | Auditoria: função genérica `capture_audit_log()` com `to_jsonb`, `TG_ARGV`, `SECURITY DEFINER`, `set_config` transacional | V29/V30/V35, `AuditActorContextAspect` | 30 triggers (10 tabelas × 3 eventos) + procedure `audit_write` com a parte comum; ator em `@app_user_id` (§3) | **alto** |
| 6 | FTS português: `to_tsvector`/`websearch_to_tsquery` + GIN | V34, `SupplierJpaRepository`, `ProductJpaRepository` | `FULLTEXT` + `MATCH ... AGAINST (... IN BOOLEAN MODE)` (§5) | **alto** |
| 7 | `pg_trgm` (email, dígitos do CNPJ) | V25, V34 | sem índice; `LIKE` com full scan | some |
| 8 | Procedures `INOUT` + `RETURNING` + `GET DIAGNOSTICS` | V36, `ReportRepositoryImpl.generate` | `OUT` + `LAST_INSERT_ID()` + `ROW_COUNT()` (§6) | baixo |
| 9 | `pg_cron` + `docker/postgres/Dockerfile` | V37 | `CREATE EVENT`; `event_scheduler` já vem `ON` no 8.4 | baixo |
| 10 | `DISTINCT ON`, `date_trunc`, `to_char`, `::`, `enum_range`, `make_interval` | V35, V38, V39, repos | `ROW_NUMBER()`, `DATE_FORMAT`, `CAST`, `stage_type + 0` (posição do ENUM) | médio |
| 11 | Coluna gerada `previous_stage_type` + FK composta auto-referente | V39 | `ENUM(...) GENERATED ALWAYS AS (...) STORED` + mesma FK (§7) | médio, **validar** |
| 12 | Sintaxe das native queries: `\|\|`, `AS "alias"`, `CAST AS BIGINT/TEXT`, `::text` | 4 repositórios | `CONCAT`, alias sem aspas ou com crase, `CAST AS SIGNED/CHAR` (§8) | médio: quebra só em runtime |
| 13 | Collation determinística do PG | geral | `utf8mb4_0900_as_cs` como padrão, `as_ci` nas colunas de busca (§9) | baixo |
| 14 | DDL transacional | Flyway | O MySQL faz commit implícito a cada DDL. Uma migration que falha no meio deixa o schema pela metade; em dev, `docker compose down -v` | aceito |
| 15 | Testcontainers `PostgreSQLContainer` + imagem customizada | testes | `MySQLContainer("mysql:8.4")` + o mesmo init dos usuários (§10) | baixo |

## 2. Baseline de migrations

O histórico PG é apagado de `src/main/resources/db/migration` e fica só no git. O baseline representa o **estado final de hoje**, já com os renames da V35, as colunas da V36 e a correção de etapas da V39:

| Arquivo | Conteúdo |
|---|---|
| `V1__create_schema.sql` | 11 tabelas com PK `id`, `created_at`/`updated_at`, ENUMs inline, `CHECK`s, FKs (inclusive a composta de `chain`), índices e `FULLTEXT` |
| `V2__seed.sql` | Dados finais de hoje, sem `audit_log` |
| `V3__create_views.sql` | `vw_stage_emission`, `vw_supplier_product_emission`, `vw_monthly_emission`, `vw_supplier_sustainability`, `vw_batch_current_stage` |
| `V4__create_procedures_and_event.sql` | `sp_generate_sustainability_report`, `sp_expire_certifications`, `ev_expire_certifications` |
| `V5__create_audit.sql` | `audit_log`, `audit_write`, as 30 triggers e os grants. Vem **depois** do seed, por isso o seed não gera eventos (RN-07) |
| `V6__grant_app_user.sql` | Grants do usuário da app, com o nome vindo do placeholder Flyway `${app_user}` |

**Seed sem reescrever à mão.** Subir o PG atual (`main`) com todas as migrations, exportar só os dados das tabelas de negócio e converter para MySQL:

```bash
pg_dump --data-only --column-inserts --exclude-table=audit_log \
        --exclude-table=flyway_schema_history supply_chain_verde > dump.sql
```

Na conversão, três cuidados:

- Remover o offset `-03` dos literais de `TIMESTAMPTZ`.
- Ordenar os `INSERT` de `chain` por `(batch_id, stage_type)`. Senão a FK auto-referente recusa a etapa que chega antes da anterior: as etapas inseridas pela V39 têm `id` maior.
- `true`/`false` continuam válidos no MySQL.

Assim os números das views continuam iguais aos de hoje, que é o que os ITs já verificam. O `AUTO_INCREMENT` se ajusta sozinho aos ids explícitos.

`.github/instructions/migrations.instructions.md` proíbe remover migrations e cita PostgreSQL. Precisa ser atualizado para registrar esta exceção única e as convenções MySQL.

## 3. Auditoria

### 3.1 Estrutura

```sql
CREATE TABLE audit_log (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT NULL,
    action             ENUM('INSERT','UPDATE','DELETE','STATUS_CHANGE') NOT NULL,
    affected_table     VARCHAR(120) NOT NULL,
    affected_entity_id BIGINT NOT NULL,
    before_data        JSON NULL,
    after_data         JSON NULL,
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_audit_log_user_id (user_id),
    INDEX idx_audit_log_created_at (created_at DESC, id DESC),
    INDEX idx_audit_log_action (action),
    INDEX idx_audit_log_affected_entity (affected_table, affected_entity_id)
);
```

A parte comum fica numa procedure só, para não repetir 30 vezes:

```sql
CREATE DEFINER = 'supply_audit'@'%' PROCEDURE audit_write(
    p_table VARCHAR(120), p_id BIGINT, p_op VARCHAR(6),
    p_before JSON, p_after JSON, p_status_changed BOOLEAN)
SQL SECURITY DEFINER
body: BEGIN
    IF @app_user_id IS NOT NULL AND @app_user_id NOT REGEXP '^[0-9]+$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid app_user_id audit context';
    END IF;
    -- RN-02: UPDATE sem mudança não gera evento.
    IF p_op = 'UPDATE' AND JSON_LENGTH(p_after) = 0 THEN
        LEAVE body;
    END IF;
    INSERT INTO audit_log (user_id, action, affected_table, affected_entity_id, before_data, after_data, created_at)
    VALUES (CAST(@app_user_id AS UNSIGNED),
            CASE WHEN p_op = 'UPDATE' AND p_status_changed THEN 'STATUS_CHANGE' ELSE p_op END,
            p_table, p_id, p_before, p_after, NOW(6));
END
```

Cada trigger só monta o JSON da sua tabela. Exemplo para `certification`, que tem coluna de estado:

```sql
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_certification_update
AFTER UPDATE ON certification FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.status <=> NEW.status) THEN
        SET b = JSON_SET(b, '$.status', OLD.status), a = JSON_SET(a, '$.status', NEW.status);
    END IF;
    IF NOT (OLD.expires_at <=> NEW.expires_at) THEN
        SET b = JSON_SET(b, '$.expires_at', audit_ts(OLD.expires_at)),
            a = JSON_SET(a, '$.expires_at', audit_ts(NEW.expires_at));
    END IF;
    -- ... uma linha IF por coluna
    CALL audit_write('certification', NEW.id, 'UPDATE', b, a, NOT (OLD.status <=> NEW.status));
END
```

- `INSERT`/`DELETE` usam `JSON_OBJECT('id', NEW.id, ...)` com **todas** as colunas, menos as sensíveis (RN-04): `users.password` simplesmente não é listada. Num schema sem tabela genérica, a lista de exclusão do PG vira "não listar".
- `STATUS_CHANGE` (RN-03): só `certification.status` e `chain.stage_type` passam `p_status_changed`; nas demais tabelas é `FALSE`.
- Formato dos valores (RF-04): `DATETIME` em `JSON_OBJECT` vira `"2026-10-22 10:00:00.000000"`, e o PG devolve `"2026-10-22T10:00:00-03:00"`. A função `audit_ts(x)` faz `DATE_FORMAT(x, '%Y-%m-%dT%H:%i:%s.%f-03:00')`. **Validar** contra o formatador do frontend. `DECIMAL` vira número JSON nos dois bancos.
- `chain.previous_stage_type` é coluna gerada e não entra no snapshot, como no PG, onde ela também não existia nos testes de auditoria. **Validar** se o PG atual inclui ou não; se incluir, listar aqui também.
- Atomicidade (RN-01): trigger e procedure rodam na mesma transação InnoDB do `INSERT`/`UPDATE`/`DELETE`, e um `SIGNAL` reverte a instrução.

### 3.2 Ator (RN-05)

No MySQL não existe `set_config(..., true)`, que é transacional. `@app_user_id` vale para a **sessão** inteira, e o Hikari reaproveita sessões. São duas camadas de proteção:

1. O `AuditActorContextAspect` continua no mesmo pointcut, mas executa `SET @app_user_id = NULLIF(?, '')` em vez do `set_config`. Toda escrita interceptada sobrescreve o valor; sem principal, grava `NULL`.
2. No mesmo advice, registrar `TransactionSynchronization.afterCompletion` executando `SET @app_user_id = NULL`. Isso cobre a escrita que não passa pelo aspect e reutiliza uma conexão suja.

**Validar no IT:** com pool de tamanho 1, escrever autenticado como A, terminar a transação, escrever por `JdbcTemplate` puro e conferir `user_id IS NULL`. Se o `afterCompletion` não tiver mais a conexão vinculada, a alternativa é limpar em `beforeCompletion`.

O job da §6 roda numa sessão do event scheduler, sem `@app_user_id`, e grava ator `NULL`, igual ao `pg_cron` hoje.

### 3.3 Privilégios (RN-06)

| Conta | Criada em | Para quê | Privilégios |
|---|---|---|---|
| `root` | imagem | Flyway (`spring.flyway.user`) | tudo, inclusive `SET_ANY_DEFINER` para criar triggers com outro DEFINER |
| `supply_audit`@`%` | `docker/mysql/init/01-users.sql`, `ACCOUNT LOCK` | DEFINER de `audit_write` e das triggers | `SELECT, TRIGGER` nas 10 tabelas; `INSERT` em `audit_log`; `EXECUTE` em `audit_write` |
| `supply_app`@`%` | mesmo init | datasource da API | `SELECT, INSERT, UPDATE, DELETE` nas 10 tabelas; **só `SELECT` em `audit_log`**; `SELECT` nas views; `EXECUTE` nas `sp_*` |

- Os grants são **por tabela**. O MySQL não tem privilégio subtrativo: um `GRANT ALL ON db.*` liberaria `audit_log`. Por isso o `MYSQL_USER` da imagem não serve, já que ele recebe `ALL` no banco inteiro.
- As senhas ficam no init e no `.env`/compose, nunca numa migration.
- As procedures `sp_*` ficam com `SQL SECURITY INVOKER`. Com o padrão `DEFINER`, elas rodariam como `root`.

## 4. Tempo

- Hoje a JVM roda em `America/Sao_Paulo`, as entidades usam `LocalDateTime` e o PG guarda `TIMESTAMPTZ`.
- No MySQL: `DATETIME(6)` com o horário de São Paulo e o servidor em `--default-time-zone=America/Sao_Paulo`. Assim `CURRENT_TIMESTAMP`, `CURRENT_DATE`, `NOW(6)` e o JDBC concordam sem conversão.
- As views trocam `CAST(ce.created_at AT TIME ZONE 'America/Sao_Paulo' AS DATE)` por `DATE(ce.created_at)`.
- Teto conhecido: `DATETIME` não guarda o instante, e isso só faria diferença se o horário de verão voltasse. A alternativa seria `TIMESTAMP`, que converte para UTC, mas para em 2038.
- O event roda `EVERY 1 DAY STARTS '... 00:00:00'` no horário de São Paulo e substitui o `0 3 * * *` UTC do `pg_cron`.
- **Validar:** a imagem `mysql:8.4` carrega as tabelas de timezone no init. Não usar `MYSQL_INITDB_SKIP_TZINFO`.
- JDBC: `jdbc:mysql://localhost:3306/supply_chain_verde?connectionTimeZone=SERVER`.

## 5. Busca textual (D-06)

```sql
ALTER TABLE supplier ADD FULLTEXT INDEX ft_supplier_name (name);
ALTER TABLE product  ADD FULLTEXT INDEX ft_product_search (name, description);
```

```sql
WHERE :search = ''
   OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
   OR (:cnpjSearch <> '' AND s.cnpj LIKE CONCAT('%', :cnpjSearch, '%'))
```

- `booleanSearch` é montado em Java: remove os operadores do modo boolean (`+ - > < ( ) ~ * " @`), separa por espaço e gera `+termo*` para cada termo. O resultado é AND com prefixo, a mesma semântica de "várias palavras" do `websearch_to_tsquery`. É uma função pura, num `static` ao lado dos repositórios, com teste unitário.
- `regexp_replace(cnpj, '[^0-9]', '')` sai, porque o `Cnpj` já grava só os 14 dígitos (`Cnpj.java:13`).
- Para `MATCH` casar sem diferenciar maiúsculas, `supplier.name`, `product.name` e `product.description` usam `COLLATE utf8mb4_0900_as_ci` (§9).
- Termos com menos de 3 letras são ignorados (`innodb_ft_min_token_size`). A lista de stopwords padrão é em inglês e não atrapalha termos em português.
- `ProductJpaRepository` faz o mesmo com `MATCH(p.name, p.description)`. A condição `lower(p.category::text) = lower(:categoryCode)` vira `p.category = :categoryCode`, porque a coluna é `ENUM` com collation `ci`.

## 6. Procedures e agendamento

```sql
CREATE PROCEDURE sp_generate_sustainability_report(
    IN p_supplier_id BIGINT, IN p_start DATE, IN p_end DATE, OUT p_report_id BIGINT)
SQL SECURITY INVOKER
BEGIN
    INSERT INTO report (supplier_id, period_start_at, period_end_at, total_co2_kg, batch_count, tracked_product_count)
    SELECT p_supplier_id, p_start, p_end, COALESCE(SUM(co2_kg), 0),
           COUNT(DISTINCT batch_id), COUNT(DISTINCT product_id)
    FROM vw_stage_emission
    WHERE supplier_id = p_supplier_id AND calculated_at BETWEEN p_start AND p_end;
    SET p_report_id = LAST_INSERT_ID();
END;

CREATE PROCEDURE sp_expire_certifications(OUT p_expired_count INT)
SQL SECURITY INVOKER
BEGIN
    UPDATE certification SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < CURRENT_DATE;
    SET p_expired_count = ROW_COUNT();
END;

CREATE EVENT ev_expire_certifications
ON SCHEDULE EVERY 1 DAY STARTS (CURRENT_DATE + INTERVAL 1 DAY)
DO CALL sp_expire_certifications(@expired);
```

`ReportRepositoryImpl.generate`: `jdbcTemplate.queryForObject("CALL ...(?, ?, ?, NULL)")` vira `SimpleJdbcCall` com o parâmetro `OUT p_report_id`. A chamada continua dentro da transação e do pointcut do aspect.

## 7. Views e ordem de etapas

| View | Mudança |
|---|---|
| `vw_stage_emission` | `DATE(ce.created_at) AS calculated_at` |
| `vw_supplier_product_emission` | nenhuma |
| `vw_monthly_emission` | `CAST(date_trunc('month', x) AS DATE)` → `DATE_FORMAT(x, '%Y-%m-01')`, convertido para `DATE` |
| `vw_supplier_sustainability` | `::NUMERIC(10,2)` → `CAST(... AS DECIMAL(10,2))` |
| `vw_batch_current_stage` | `DISTINCT ON` → subquery com `ROW_NUMBER() OVER (PARTITION BY batch_id ORDER BY started_at DESC, id DESC) = 1` |

`chain` (RN-09):

```sql
previous_stage_type ENUM('PRODUCTION','STORAGE','PROCESSING','TRANSPORT','DISTRIBUTION','RETAIL')
    GENERATED ALWAYS AS (CASE stage_type
        WHEN 'STORAGE' THEN 'PRODUCTION' WHEN 'PROCESSING' THEN 'STORAGE'
        WHEN 'TRANSPORT' THEN 'PROCESSING' WHEN 'DISTRIBUTION' THEN 'TRANSPORT'
        WHEN 'RETAIL' THEN 'DISTRIBUTION' END) STORED,
CONSTRAINT uq_chain_batch_stage UNIQUE (batch_id, stage_type),
CONSTRAINT fk_chain_previous_stage FOREIGN KEY (batch_id, previous_stage_type)
    REFERENCES chain (batch_id, stage_type)
```

- O MySQL aceita FK em coluna gerada `STORED`, desde que sem `ON UPDATE CASCADE/SET NULL`, e o padrão é `RESTRICT`.
- Com `NULL` numa das colunas da FK, o InnoDB não verifica a linha. É o mesmo `MATCH SIMPLE` do PG, então `PRODUCTION` entra sem dependência.
- **Validar no `StageOrderIT`:** pular etapa, repetir etapa e apagar etapa do meio continuam recusados.
- `GlobalExceptionHandler` identifica a violação pelo nome da constraint. A mensagem do MySQL (`Cannot add or update a child row: ... CONSTRAINT \`fk_chain_previous_stage\``) também traz o nome, mas **conferir** como ele extrai: se usa `SQLState`/código PG, precisa trocar para MySQL (`23000`, erros 1062 e 1452).

## 8. Native queries

| Arquivo | Trocar |
|---|---|
| `SupplierJpaRepository` (5 queries) | FTS (§5); `'%' \|\| :x \|\| '%'` → `CONCAT('%', :x, '%')`; `AS "supplierId"` → `` AS `supplierId` ``; `CAST(:productId AS BIGINT)` → `CAST(:productId AS SIGNED)`; `CAST(:category AS product_category)` → `:category`; `CAST(x AS INTEGER)` → `CAST(x AS SIGNED)` |
| `ProductJpaRepository` | FTS (§5); `::text` sai |
| `BatchJpaRepository` | `cast(x as text)` → `CAST(x AS CHAR)`; aliases entre crases |
| `CarbonEmissionJpaRepository` | `to_char(month, 'YYYY-MM')` → `DATE_FORMAT(month, '%Y-%m')`; `CAST(:supplierId AS BIGINT)` → `SIGNED`; `CAST(stage_type AS TEXT)` → `CHAR`; aliases entre crases |
| `AuditLogRepositoryImpl`, `UserRepositoryImpl` | JPQL portável; `LOWER(email) LIKE` continua igual |

O `\|\|` e as aspas duplas **não dão erro de sintaxe no MySQL**: viram OR lógico e string literal. Por isso a cobertura de testes de cada query é o que garante a troca (D-08).

Alternativa rejeitada: ligar `sql_mode=ANSI_QUOTES,PIPES_AS_CONCAT` na conexão para não mexer nessas queries. Ela esconde a diferença de dialeto numa configuração, e as queries de FTS precisam ser reescritas de qualquer jeito.

## 9. Collation (D-07)

| Onde | Collation | Por quê |
|---|---|---|
| Padrão do banco (`CREATE DATABASE ... COLLATE`) | `utf8mb4_0900_as_cs` | Diferencia maiúscula e acento, como o PG |
| `users.email`, `supplier.cnpj` | padrão (`as_cs`) | `UNIQUE` igual ao de hoje; a busca por email já usa `LOWER(...) LIKE` |
| `supplier.name`, `product.name`, `product.description` | `utf8mb4_0900_as_ci` | O FTS do PG ignora maiúsculas e respeita acentos (config `portuguese`, sem `unaccent`) |
| ENUMs | `utf8mb4_0900_as_ci` | `category = :categoryCode` sem `lower()` |

Diferença residual: a **ordem** de `ORDER BY name` segue as regras do ICU do MySQL, não as do glibc do PG. Nenhum endpoint ordena por texto como regra de negócio.

## 10. Infra, dependências e testes

- `pom.xml`:
  - sai `org.postgresql:postgresql` e entra `com.mysql:mysql-connector-j`;
  - sai `flyway-database-postgresql` e entra `flyway-mysql`;
  - sai `testcontainers-postgresql` e entra `testcontainers-mysql`;
  - a propriedade `flyway.url` passa para `jdbc:mysql://...`.
- `docker-compose.yml`:
  - imagem `mysql:8.4`, porta `3306`;
  - `command: --default-time-zone=America/Sao_Paulo --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_cs`;
  - volume `./docker/mysql/init:/docker-entrypoint-initdb.d`;
  - healthcheck com `mysqladmin ping`.
- `docker/postgres/` sai.
- `application.properties`:
  - URL MySQL e `driver-class-name=com.mysql.cj.jdbc.Driver`;
  - `spring.datasource.username=supply_app`;
  - `spring.flyway.user`/`password` de admin;
  - `spring.flyway.placeholders.app_user=supply_app`.
- Entidades JPA: tirar `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`; `AuditLogJpaEntity` passa a ter `columnDefinition = "json"`. O `ddl-auto=validate` confirma o mapeamento. **Validar** se o Hibernate 7, com o dialeto MySQL, aceita a coluna `ENUM(...)` para `@Enumerated(STRING)` sem `columnDefinition`.
- `TestcontainersConfiguration`:
  - `MySQLContainer("mysql:8.4")` com o mesmo `command` e o init copiado para `/docker-entrypoint-initdb.d`;
  - Flyway como `root` e datasource como `supply_app`. Como `@ServiceConnection` entrega um único usuário, **validar** se dá para sobrescrever o usuário do datasource; se não der, trocar por `@DynamicPropertySource`.
- Testes que escrevem SQL PG direto e precisam ser portados: `AuditLogTriggerIT` (`::TEXT`), `ViewsAndProceduresIT`, `StageOrderIT`, `DashboardEmissionsIT`, `SupplierRecommendationIT`, `ReportDetailIT`, `SupplierUserLinkIT`, `SupplierOwnProfileIT` e `src/test/resources/seed/report-seed.sql`.
- Casos novos:
  - privilégio: `supply_app` recebe erro ao fazer `INSERT INTO audit_log`;
  - ator não vaza entre conexões (§3.2);
  - `UPDATE` sem mudança não gera evento nem altera `updated_at`;
  - event: a definição existe e `CALL sp_expire_certifications(@n)` expira;
  - helper de busca boolean (unitário).

## 11. Pontos a validar antes de fechar

| # | O quê | Como |
|---|---|---|
| V-1 | FK composta em coluna gerada `STORED` + auto-referência | `StageOrderIT` |
| V-2 | Limpeza de `@app_user_id` no fim da transação | IT com pool 1 (§3.2) |
| V-3 | Triggers com DEFINER bloqueado gravam e a app não grava direto | `AuditLogTriggerIT` + caso novo |
| V-4 | Formato ISO das datas no snapshot bate com o frontend | Comparar com o `afterData` de hoje |
| V-5 | `ENUM` nativo + `@Enumerated(STRING)` passa no `ddl-auto=validate` | Subir a app |
| V-6 | Tabelas de timezone presentes na imagem | `SELECT CONVERT_TZ(NOW(), 'UTC', 'America/Sao_Paulo')` não nulo |
| V-7 | Testcontainers com dois usuários | `./mvnw verify` |
| V-8 | Mensagens de violação no `GlobalExceptionHandler` | `StageOrderIT`, testes de conflito de CNPJ e email |
| V-9 | Números das views e do relatório iguais aos do PG | `ViewsAndProceduresIT`, `DashboardEmissionsIT` sem mudar o esperado |
