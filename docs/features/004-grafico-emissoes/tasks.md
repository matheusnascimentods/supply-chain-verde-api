# Tarefas — Série mensal de emissões

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** não iniciado. Depende de [`vinculo-usuario-fornecedor`](../002-vinculo-usuario-fornecedor/tasks.md) (Task 2, `supplierId` no token) e [`views-procedures`](../003-views-procedures/tasks.md) (Task 1).

---

## Task 1 — `feat/dashboard-monthly-emissions`

- [ ] `MonthlyEmissionDTO` e `GetMonthlyEmissionsUseCase`
- [ ] Consulta na `vw_monthly_emission` com filtro opcional de fornecedor
- [ ] `GET /dashboard/emissions` com escopo por role
- [ ] Testes do plan
- [ ] Atualizar a tabela de contratos em `docs/features/000-base/spec.md`
