# Tarefas — Padronização do schema

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md). Cada task é um branch/PR para `main`, em sequência.

**Status:** não iniciado. A [views-procedures](../006-views-procedures/tasks.md) começa depois desta, já com os nomes novos.

---

## Task 1 — `refactor/search-expression-indexes`

- [ ] `V34__replace_search_columns_with_expression_indexes.sql`
- [ ] `SupplierJpaRepository` e `ProductJpaRepository` usam as expressões dos índices
- [ ] `EXPLAIN` da busca de fornecedor e de produto mostra `Bitmap Index Scan` nos índices novos
- [ ] Testes de busca existentes passando sem mudar as asserções

## Task 2 — `refactor/standardize-ids-and-timestamps`

Tudo num PR só: a `V35` e o código não funcionam um sem o outro.

- [ ] Salvar o `/v3/api-docs` da `main` antes de começar
- [ ] `V35__standardize_ids_and_timestamps.sql` (passos 1 a 9 do plan)
- [ ] `TimeZone.setDefault` em `SupplyChainVerdeApplication`
- [ ] `@Column` das 11 entidades JPA e `@CreationTimestamp` nas colunas de criação
- [ ] Conversão `LocalDateTime → LocalDate` em `SupplierJpaMapper` e `CarbonEmissionJpaMapper`
- [ ] Queries nativas de `SupplierJpaRepository`, `ProductJpaRepository` e `BatchJpaRepository`
- [ ] `sumCo2KgByCalculatedAtBetween` lê a `vw_stage_emission`
- [ ] SQL de preparação dos testes com os nomes novos
- [ ] Testes novos: `updated_at` no update e no no-op, chave `id` na auditoria, reescrita do histórico
- [ ] `diff` do `/v3/api-docs` vazio

**Pronto:** `./mvnw verify` verde sem mudar asserções de resposta, `diff` do `/v3/api-docs` vazio e `\d supplier` mostrando `id`, `created_at` e `updated_at` (`timestamptz`) e nenhuma coluna de busca.
