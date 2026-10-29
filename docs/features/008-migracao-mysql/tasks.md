# Tarefas — Migração de PostgreSQL para MySQL

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** concluído. As tasks foram entregues juntas, porque um banco não roda com o código do outro.

Desvios do plano:

- `ReportRepositoryImpl.generate` usa `CALL ...(?, ?, ?, @report_id)` + `SELECT @report_id` na conexão da transação, e não `SimpleJdbcCall`: a leitura de metadados da procedure dependeria de privilégio em `information_schema` que `supply_app` não tem.
- O seed (V2) desliga `FOREIGN_KEY_CHECKS` em vez de ordenar `chain`: os dados vieram de um banco com as mesmas FKs.
- `updated_at` fica fora do delta de `UPDATE` na auditoria (muda junto com qualquer coluna).
- `SupplierScore.activeCertificationCount` virou `Long`: o MySQL não tem `CAST ... AS INT`, e `COUNT` é `BIGINT`. O contrato HTTP não muda.
- O helper de busca também remove as stopwords padrão do InnoDB, que com `+` zeram o resultado.
- O FULLTEXT do InnoDB só vê linhas commitadas: `SupplierRecommendationIT` isola os dados pela raiz do CNPJ, e a busca por nome tem um teste que faz commit.
- `ProductControllerIT` já falhava no PostgreSQL (`deleteAll()` bate na FK dos seeds) e continua falhando; o CI não roda `*IT`.

---

## Task 1 — `chore/mysql-infra`

Infra e dependências, ainda sem migrations.

- [x] `pom.xml`: driver, Flyway e Testcontainers do MySQL (plan §10); remover os de PostgreSQL
- [x] `docker-compose.yml` com `mysql:8.4` e `command` de timezone, charset e collation
- [x] `docker/mysql/init/01-users.sql`: `supply_audit` (`ACCOUNT LOCK`) e `supply_app`
- [x] Remover `docker/postgres/`
- [x] `application.properties`: URL, driver, usuário da app, usuário do Flyway, placeholder `app_user`
- [x] `TestcontainersConfiguration` com `MySQLContainer` e dois usuários (V-7)

**Pronto:** `docker compose up` sobe o MySQL; `SELECT CONVERT_TZ(NOW(), 'UTC', 'America/Sao_Paulo')` não é nulo (V-6); `supply_app` consegue conectar.

## Task 2 — `feat/mysql-baseline-schema`

- [x] Apagar `V1`–`V39` PostgreSQL
- [x] `V1__create_schema.sql`: tabelas, ENUMs inline, `CHECK`s, FKs, índices, `FULLTEXT`, `ON UPDATE CURRENT_TIMESTAMP(6)`, `previous_stage_type` + FK composta (plan §2, §7, §9)
- [x] `V2__seed.sql` gerado do dump do PG final, com `chain` ordenada por `(batch_id, stage_type)` (plan §2)
- [x] `V3__create_views.sql` (plan §7)
- [x] `V4__create_procedures_and_event.sql` (plan §6)
- [x] Entidades JPA sem `@JdbcTypeCode(NAMED_ENUM)`; `AuditLogJpaEntity` com `json` (V-5)
- [x] Atualizar `.github/instructions/migrations.instructions.md`

**Pronto:** a app sobe com `ddl-auto=validate`; `StageOrderIT` verde (V-1).

## Task 3 — `feat/mysql-audit-triggers`

- [x] `V5__create_audit.sql`: `audit_log`, `audit_ts`, `audit_write` e 30 triggers com `DEFINER = supply_audit` (plan §3.1)
- [x] `V6__grant_app_user.sql`: grants por tabela; só `SELECT` em `audit_log` (plan §3.3)
- [x] `AuditActorContextAspect`: `SET @app_user_id` + limpeza ao fim da transação (plan §3.2)
- [x] Portar `AuditLogTriggerIT` + casos novos: privilégio negado, ator sem vazamento, `UPDATE` sem mudança (V-2, V-3)
- [x] Conferir o formato das datas no snapshot com o frontend (V-4)

**Pronto:** `AuditLogTriggerIT` verde com pool de tamanho 1.

## Task 4 — `refactor/mysql-native-queries`

- [x] Helper de busca boolean (`+termo*`) + teste unitário (plan §5)
- [x] `SupplierJpaRepository`, `ProductJpaRepository`, `BatchJpaRepository`, `CarbonEmissionJpaRepository` (plan §8)
- [x] `ReportRepositoryImpl.generate` com `SimpleJdbcCall` (plan §6)
- [x] `GlobalExceptionHandler`: violações pelo código MySQL (V-8)
- [x] Portar o SQL direto dos ITs e de `src/test/resources/seed/report-seed.sql`

**Pronto:** `./mvnw verify` verde, sem mudar números esperados nos testes de views e relatório (V-9).

## Task 5 — `docs/mysql`

- [x] `docs/adr/0002-migracao-para-mysql.md`: substitui o contrato transacional da ADR 0001 (ator por sessão + limpeza, DEFINER no lugar de role, busca sem stemming)
- [x] ADR 0001: status "substituída parcialmente pela 0002"
- [x] README: badge, stack, como subir, variáveis, seções de auditoria, `pg_cron` e ENUM
- [x] `docs/sql/consultas-relatorio.sql` em sintaxe MySQL
- [x] Marcar o status desta feature como concluído

**Pronto:** `grep -ri postgres` só encontra o histórico (ADR 0001, specs 000–007).
