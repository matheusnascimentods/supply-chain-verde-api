# Tarefas — Views e procedures

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md). Cada task é um branch/PR para `main`, em sequência.

**Status:** não iniciado. Depende de [`vinculo-usuario-fornecedor`](../vinculo-usuario-fornecedor/tasks.md) apenas pela numeração das migrations.

---

## Task 1 — `feat/emission-views`

- [ ] Migration com `vw_stage_emission`, `vw_supplier_product_emission`, `vw_monthly_emission`
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
- [ ] Citar views e procedures no `docs/spec.md` e no README

**Pronto:** relatório gerado pela API com `totalCo2Kg` diferente de zero para um fornecedor com emissões no período.
