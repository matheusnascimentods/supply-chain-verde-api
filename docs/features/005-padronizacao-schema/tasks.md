# Tarefas — Padronização do schema

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** concluído, na branch `refactor/padronizacao-schema` (um commit por task). A [views-procedures](../006-views-procedures/tasks.md) começa depois desta, já com os nomes novos.

---

## Task 1 — índices de busca

- [x] `V34__replace_search_columns_with_expression_indexes.sql`
- [x] `SupplierJpaRepository` e `ProductJpaRepository` usam as expressões dos índices
- [x] `EXPLAIN` da busca de fornecedor e de produto mostra `Bitmap Index Scan` nos índices novos
- [x] Testes de busca existentes passando sem mudar as asserções

## Task 2 — PK `id` e `created_at`/`updated_at`

Tudo num commit só: a `V35` e o código não funcionam um sem o outro.

- [x] Salvar o `/v3/api-docs` da `main` antes de começar
- [x] `V35__standardize_ids_and_timestamps.sql` (passos 1 a 9 do plan)
- [x] `TimeZone.setDefault` em `SupplyChainVerdeApplication`
- [x] `@Column` das 11 entidades JPA e `@CreationTimestamp` nas colunas de criação
- [x] Conversão `LocalDateTime → LocalDate` em `SupplierJpaMapper` e `CarbonEmissionJpaMapper`
- [x] Queries nativas de `SupplierJpaRepository`, `ProductJpaRepository` e `BatchJpaRepository`
- [x] `sumCo2KgByCalculatedAtBetween` lê a `vw_stage_emission`
- [x] SQL de preparação dos testes com os nomes novos
- [x] Testes novos: `updated_at` no update e no no-op, chave `id` na auditoria. A reescrita do histórico foi verificada à mão (ver plan, §3)
- [x] `diff` do `/v3/api-docs` vazio

**Pronto:** `./mvnw verify` verde sem mudar asserções de resposta, `diff` do `/v3/api-docs` vazio e `\d supplier` mostrando `id`, `created_at` e `updated_at` (`timestamptz`) e nenhuma coluna de busca.
