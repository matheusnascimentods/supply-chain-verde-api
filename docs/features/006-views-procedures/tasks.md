# Tarefas — Views e procedures

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md). Cada task é um branch/PR para `main`, em sequência.

**Status:** não iniciado. Depende da [padronização do schema](../005-padronizacao-schema/tasks.md): as procedures já nascem com `id` como PK e a numeração começa na `V36`.

---

## Task 1 — `feat/emission-views`

- [x] Migration com `vw_stage_emission`, `vw_supplier_product_emission` (`V31__create_emission_views.sql`) e `vw_monthly_emission` (`V33__create_monthly_emission_view.sql`, criada no [gráfico de emissões](../003-grafico-emissoes/tasks.md))
- [ ] Testes de integração das views
- [ ] `GetDashboardSummaryUseCase` lê `monthlyEmissionKgCo2e` da `vw_monthly_emission`

## Task 2 — `fix/report-totals-procedure`

- [ ] Migration com `sp_generate_sustainability_report`
- [ ] `GenerateSustainabilityReportUseCase` chama a procedure e relê o `report` gravado
- [ ] Testes: totais preenchidos, fornecedor sem emissão, auditoria registrada

## Task 3 — `feat/expire-certifications-job`

- [ ] Migration com `sp_expire_certifications`
- [ ] `@EnableScheduling` + job diário
- [ ] Testes da procedure e log do job com a quantidade expirada

## Task 4 — `docs/sql-report-queries`

- [ ] `docs/sql/consultas-relatorio.sql` com as consultas do plan
- [ ] Citar views e procedures no `docs/features/000-base/spec.md` e no README

**Pronto:** relatório gerado pela API com `totalCo2Kg` diferente de zero para um fornecedor com emissões no período.
