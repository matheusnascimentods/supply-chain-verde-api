# Tarefas — Série mensal de emissões

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** concluído. A `vw_monthly_emission` foi criada nesta feature (`V33__create_monthly_emission_view.sql`), porque a `V31` só tinha as outras duas views. Depende de [`vinculo-usuario-fornecedor`](../002-vinculo-usuario-fornecedor/tasks.md) (Task 2, `supplierId` no token) e [`views-procedures`](../006-views-procedures/tasks.md) (Task 1).

---

## Task 1 — `feat/dashboard-monthly-emissions`

- [x] `MonthlyEmissionDTO` e `GetMonthlyEmissionsUseCase`
- [x] Consulta na `vw_monthly_emission` com filtro opcional de fornecedor
- [x] `GET /dashboard/emissions` com escopo por role
- [x] Testes do plan
- [x] Atualizar a tabela de contratos em `docs/features/000-base/spec.md`
