# Tarefas — Série mensal de emissões

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** não iniciado. Depende de [`vinculo-usuario-fornecedor`](../vinculo-usuario-fornecedor/tasks.md) (Task 2, `supplierId` no token) e [`views-procedures`](../views-procedures/tasks.md) (Task 1).

---

## Task 1 — `feat/dashboard-monthly-emissions`

- [ ] `MonthlyEmissionDTO` e `GetMonthlyEmissionsUseCase`
- [ ] Consulta na `vw_monthly_emission` com filtro opcional de fornecedor
- [ ] `GET /dashboard/emissions` com escopo por role
- [ ] Testes do plan
- [ ] Atualizar a tabela de contratos em `docs/spec.md`
