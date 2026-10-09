# Tarefas — Recomendação de fornecedores sustentáveis

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** não iniciado. Depende de [`views-procedures`](../views-procedures/tasks.md) (Task 1).

---

## Task 1 — `feat/supplier-recommendation`

- [ ] Parâmetros `productId`, `category`, `unit` em `GET /suppliers?ranked=true` com validação de combinação
- [ ] Consultas nativas ordenadas por `co2_kg_per_unit` (`NULLS LAST`) + contagem
- [ ] `co2KgPerUnit` em `SupplierRankingDTO`
- [ ] Desempate por `sustainabilityScore` na página
- [ ] Testes do plan
- [ ] Atualizar a tabela de contratos em `docs/spec.md`

**Pronto:** `GET /suppliers?ranked=true&category=AGRICULTURE&unit=KG` devolve primeiro o fornecedor de menor CO₂ por kg do seed.
