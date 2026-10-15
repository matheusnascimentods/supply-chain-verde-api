# Tarefas — Detalhe do relatório para o PDF

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** concluído. As seções leem a `vw_stage_emission` da `V31`; o `totalCo2Kg` do cabeçalho só fica preenchido com a procedure da [`views-procedures`](../006-views-procedures/tasks.md) (Task 2).

---

## Task 1 — `feat/report-detail-sections`

- [x] Records aninhados no `ReportDetailDTO`
- [x] Consultas por etapa, lote e metodologia na `vw_stage_emission`
- [x] Certificações vigentes no período
- [x] `GetReportUseCase` monta as seções
- [x] Testes do plan e Swagger
