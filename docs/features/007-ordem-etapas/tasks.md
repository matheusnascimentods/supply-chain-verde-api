# Tarefas — Ordem obrigatória das etapas

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** não iniciado. Depende da [006](../006-views-procedures/tasks.md) só pela numeração (`V39` depois da `V38`).

---

## Task 1 — `feat/stage-order-constraints`

Tudo num commit só: sem a validação na API, a constraint devolveria 500.

- [ ] `V39__enforce_stage_order.sql` (correção dos dados + coluna gerada + `UNIQUE` + FK)
- [ ] `StageType.next()`
- [ ] `RegisterChainStageUseCase` valida a transição e lança `InvalidStageTransitionException`
- [ ] `GlobalExceptionHandler` trata a violação das duas constraints
- [ ] Testes do plan, §4
- [ ] Atualizar regras de etapa em `docs/features/000-base/spec.md`

**Pronto:** `./mvnw verify` verde; `POST` de etapa fora de ordem responde com erro de transição; `INSERT` manual fora de ordem no `psql` falha em `fk_chain_previous_stage`.
