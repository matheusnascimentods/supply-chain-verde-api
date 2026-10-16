# Tarefas — Views e procedures

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md). Cada task é um branch/PR para `main`, em sequência.

**Status:** não iniciado. Depende da [padronização do schema](../005-padronizacao-schema/tasks.md): as procedures já nascem com `id` como PK e a numeração começa na `V36`.

---

## Task 1 — `feat/emission-views`

- [x] Migration com `vw_stage_emission`, `vw_supplier_product_emission` (`V31__create_emission_views.sql`) e `vw_monthly_emission` (`V33__create_monthly_emission_view.sql`, criada no [gráfico de emissões](../003-grafico-emissoes/tasks.md))
- [ ] Testes de integração das views
- [ ] `GetDashboardSummaryUseCase` lê `monthlyEmissionKgCo2e` da `vw_monthly_emission`

## Task 2 — `fix/report-totals-procedure`

- [ ] Migration com `report.batch_count` e `sp_generate_sustainability_report`
- [ ] `ListReportsUseCase` e `GetReportUseCase` leem `batch_count` (sai o N+1)
- [ ] Frontend (`supply-chain-verde-web`): aviso de que o relatório é um retrato do momento da geração
- [ ] `GenerateSustainabilityReportUseCase` chama a procedure e relê o `report` gravado
- [ ] Testes: totais preenchidos, fornecedor sem emissão, auditoria registrada

## Task 3 — `feat/expire-certifications-job`

- [ ] Migration com `sp_expire_certifications`
- [ ] Imagem `docker/postgres` com `pg_cron`, usada no compose e no Testcontainers (plan, §4.1)
- [ ] `V37` com `CREATE EXTENSION pg_cron` e `cron.schedule`
- [ ] Testes: procedure (só `ACTIVE` vencidas mudam) e job registrado em `cron.job`
- [ ] RN-08 em `RegisterCertificationUseCase` e RN-09 em `UpdateCertificationStatusUseCase` (`ValidationException` → 400)

## Task 4 — `refactor/sustainability-ranking-view`

- [ ] `V38` com `vw_supplier_sustainability` e `vw_batch_current_stage`
- [ ] Ranking lê a view, pagina no SQL e busca certificações da página em lote
- [ ] Apagar `SustainabilityScoreCalculator` e `SustainabilityScoreCalculatorTest`; casos migram para teste de integração da view
- [ ] `countActiveBatches` e `findRecentWithLatestStage` leem `vw_batch_current_stage`
- [ ] Respostas de `GET /suppliers/ranking` e do dashboard iguais às de antes (mesma ordem e mesmos valores)

## Task 5 — `docs/sql-report-queries`

- [ ] `docs/sql/consultas-relatorio.sql` com as consultas do plan
- [ ] Citar views e procedures no `docs/features/000-base/spec.md` e no README

**Pronto:** relatório gerado pela API com `totalCo2Kg` diferente de zero para um fornecedor com emissões no período, e ranking sem consulta por fornecedor.

A ordem obrigatória das etapas (constraints + state machine) está na [007](../007-ordem-etapas/spec.md).
