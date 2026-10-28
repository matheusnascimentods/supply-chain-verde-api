# Tarefas — Migração de PostgreSQL para MySQL

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** planejado, nada implementado. As tasks são sequenciais: não dá para entregar pela metade, porque um banco não roda com o código do outro. São várias branches empilhadas, ou uma branch com um commit por task e um PR só no fim.

---

## Task 1 — `chore/mysql-infra`

Infra e dependências, ainda sem migrations.

- [ ] `pom.xml`: driver, Flyway e Testcontainers do MySQL (plan §10); remover os de PostgreSQL
- [ ] `docker-compose.yml` com `mysql:8.4` e `command` de timezone, charset e collation
- [ ] `docker/mysql/init/01-users.sql`: `supply_audit` (`ACCOUNT LOCK`) e `supply_app`
- [ ] Remover `docker/postgres/`
- [ ] `application.properties`: URL, driver, usuário da app, usuário do Flyway, placeholder `app_user`
- [ ] `TestcontainersConfiguration` com `MySQLContainer` e dois usuários (V-7)

**Pronto:** `docker compose up` sobe o MySQL; `SELECT CONVERT_TZ(NOW(), 'UTC', 'America/Sao_Paulo')` não é nulo (V-6); `supply_app` consegue conectar.

## Task 2 — `feat/mysql-baseline-schema`

- [ ] Apagar `V1`–`V39` PostgreSQL
- [ ] `V1__create_schema.sql`: tabelas, ENUMs inline, `CHECK`s, FKs, índices, `FULLTEXT`, `ON UPDATE CURRENT_TIMESTAMP(6)`, `previous_stage_type` + FK composta (plan §2, §7, §9)
- [ ] `V2__seed.sql` gerado do dump do PG final, com `chain` ordenada por `(batch_id, stage_type)` (plan §2)
- [ ] `V3__create_views.sql` (plan §7)
- [ ] `V4__create_procedures_and_event.sql` (plan §6)
- [ ] Entidades JPA sem `@JdbcTypeCode(NAMED_ENUM)`; `AuditLogJpaEntity` com `json` (V-5)
- [ ] Atualizar `.github/instructions/migrations.instructions.md`

**Pronto:** a app sobe com `ddl-auto=validate`; `StageOrderIT` verde (V-1).

## Task 3 — `feat/mysql-audit-triggers`

- [ ] `V5__create_audit.sql`: `audit_log`, `audit_ts`, `audit_write` e 30 triggers com `DEFINER = supply_audit` (plan §3.1)
- [ ] `V6__grant_app_user.sql`: grants por tabela; só `SELECT` em `audit_log` (plan §3.3)
- [ ] `AuditActorContextAspect`: `SET @app_user_id` + limpeza ao fim da transação (plan §3.2)
- [ ] Portar `AuditLogTriggerIT` + casos novos: privilégio negado, ator sem vazamento, `UPDATE` sem mudança (V-2, V-3)
- [ ] Conferir o formato das datas no snapshot com o frontend (V-4)

**Pronto:** `AuditLogTriggerIT` verde com pool de tamanho 1.

## Task 4 — `refactor/mysql-native-queries`

- [ ] Helper de busca boolean (`+termo*`) + teste unitário (plan §5)
- [ ] `SupplierJpaRepository`, `ProductJpaRepository`, `BatchJpaRepository`, `CarbonEmissionJpaRepository` (plan §8)
- [ ] `ReportRepositoryImpl.generate` com `SimpleJdbcCall` (plan §6)
- [ ] `GlobalExceptionHandler`: violações pelo código MySQL (V-8)
- [ ] Portar o SQL direto dos ITs e de `src/test/resources/seed/report-seed.sql`

**Pronto:** `./mvnw verify` verde, sem mudar números esperados nos testes de views e relatório (V-9).

## Task 5 — `docs/mysql`

- [ ] `docs/adr/0002-migracao-para-mysql.md`: substitui o contrato transacional da ADR 0001 (ator por sessão + limpeza, DEFINER no lugar de role, busca sem stemming)
- [ ] ADR 0001: status "substituída parcialmente pela 0002"
- [ ] README: badge, stack, como subir, variáveis, seções de auditoria, `pg_cron` e ENUM
- [ ] `docs/sql/consultas-relatorio.sql` em sintaxe MySQL
- [ ] Marcar o status desta feature como concluído

**Pronto:** `grep -ri postgres` só encontra o histórico (ADR 0001, specs 000–007).
