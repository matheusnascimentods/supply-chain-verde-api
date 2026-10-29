# Migração do banco para MySQL 8.4

*Template: decision record de Jeff Tyree e Art Akerman, “Architecture Decisions: Demystifying Architecture”.*

## Issue

O projeto é acadêmico e a disciplina exige MySQL. A ADR 0001 fixou a auditoria por trigger no PostgreSQL, com o ator em `set_config('app.user_id', ..., true)` e uma função genérica `SECURITY DEFINER`. Outras regras de dados usavam recursos exclusivos do PostgreSQL: busca textual em português, `pg_trgm`, `ENUM`s nomeados, `DISTINCT ON`, procedures com `INOUT` e `pg_cron`. A troca precisa manter as regras no banco sem perder features. Requisitos em [`../features/008-migracao-mysql/spec.md`](../features/008-migracao-mysql/spec.md).

## Decision

Trocar o banco por MySQL 8.4 LTS, com um baseline consolidado (V1–V6) no lugar das 39 migrations PostgreSQL. A auditoria **continua no banco, por trigger** (a decisão da ADR 0001 vale), mas três pontos do contrato dela mudam:

1. **Ator por sessão, limpo ao fim da transação.** O MySQL não tem configuração local à transação. O `AuditActorContextAspect` executa `SET @app_user_id = :userId` antes de cada escrita auditada e registra um `afterCompletion` que executa `SET @app_user_id = NULL` na mesma conexão.
2. **DEFINER no lugar de role owner.** As 30 triggers (10 tabelas × 3 eventos) e a procedure `audit_write` têm `DEFINER = supply_audit`, conta com `ACCOUNT LOCK`. Só ela tem `INSERT` em `audit_log`. O usuário da API (`supply_app`) recebe grants por tabela e só `SELECT` em `audit_log`.
3. **Busca sem stemming.** `FULLTEXT` + `MATCH ... AGAINST` em `BOOLEAN MODE`, com cada palavra obrigatória e por prefixo (`+termo*`).

## Status

**Substitui parcialmente a [ADR 0001](0001-auditoria-no-postgresql.md)** nos pontos acima. Implementada e validada contra MySQL 8.4 via Testcontainers.

## Group

Dados, integração e segurança.

## Assumptions

- Todo ambiente nasce do zero: não há cópia de dados do PostgreSQL.
- O Flyway roda com uma conta admin (`root`), que cria triggers com outro DEFINER (`SET_ANY_DEFINER`) e dá grants. As contas `supply_app` e `supply_audit` são criadas fora das migrations (`docker/mysql/init`), para as senhas não irem ao Flyway.
- O servidor roda em `America/Sao_Paulo`, como a JVM.

## Constraints

- Mantidas da ADR 0001: ator vem do principal validado; auditoria atômica com a escrita; a API não escreve em `audit_log`; snapshots sem segredos; `audit_log` começa vazia e seeds não geram eventos.
- O MySQL não tem privilégio subtrativo: um `GRANT ALL` no banco liberaria `audit_log`. Os grants são por tabela, e o `MYSQL_USER` da imagem não é usado.

## Positions

1. **Ator em variável de sessão + limpeza no `afterCompletion`.** Escolhida.
2. **Ator numa tabela de contexto por `CONNECTION_ID()`.** Mesmo problema de limpeza, com uma tabela a mais.
3. **Ligar `sql_mode=ANSI_QUOTES,PIPES_AS_CONCAT` para não reescrever native queries.** Rejeitada: esconde o dialeto numa configuração, e as queries de busca precisam ser reescritas de qualquer jeito.

## Argument

`@app_user_id` vale para a sessão inteira, e o Hikari reaproveita sessões. Duas camadas impedem o vazamento: toda escrita interceptada sobrescreve o valor (sem principal, grava `NULL`), e o `afterCompletion` limpa a variável antes de a conexão voltar ao pool. No `afterCompletion` a conexão da transação ainda está vinculada à thread; o `beforeCompletion` não serve, porque o Hibernate faz o flush depois dele. O `AuditLogTriggerIT` cobre o caso com pool de uma conexão.

O `DEFINER` bloqueado é o equivalente MySQL do owner `NOLOGIN` do PostgreSQL: a trigger roda com os privilégios dele, e ninguém faz login com ele.

## Implications

- `UPDATE` grava só as colunas alteradas, comparadas uma a uma na trigger. `updated_at` fica de fora do delta: ele muda junto com qualquer coluna. Colunas `_ci` (`supplier.name`, `product.name`, `product.description`) são comparadas com `BINARY`, para uma troca só de maiúsculas contar como mudança.
- Datas no snapshot seguem ISO 8601 com offset fixo `-03:00` (`audit_ts`), o mesmo formato do `to_jsonb` de `TIMESTAMPTZ`. São Paulo não tem horário de verão; se voltar a ter, o offset precisa vir de `CONVERT_TZ`.
- `chain.previous_stage_type` (coluna gerada) entra nos snapshots, como entrava no PostgreSQL.
- O FULLTEXT do InnoDB só enxerga linhas commitadas, e as mudanças dentro da mesma transação não aparecem na busca. Palavras com menos de 3 letras e as stopwords padrão do InnoDB são removidas antes de montar a busca: com `+`, elas zerariam o resultado.
- `updated_at` é nativo (`ON UPDATE CURRENT_TIMESTAMP(6)`), e o trigger `set_updated_at` deixa de existir.
- O `EVENT ev_expire_certifications` substitui o `pg_cron` e roda à meia-noite de São Paulo, sem ator.
- DDL no MySQL não é transacional: migration que falha no meio deixa o schema pela metade.

## Related artifacts

- [`../features/008-migracao-mysql/plan.md`](../features/008-migracao-mysql/plan.md): inventário PG → MySQL e desenho de cada parte.
- `src/main/resources/db/migration/V5__create_audit.sql` e `V6__grant_app_user.sql`.
- `AuditActorContextAspect` e `AuditLogTriggerIT`.
