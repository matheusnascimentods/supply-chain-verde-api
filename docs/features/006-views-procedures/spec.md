# Especificação — Views e procedures

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Atende ao entregável #4 da A3 (views, stored procedures e consultas para relatórios em SQL).

## 1. Problema

- Os cálculos de sustentabilidade (CO₂ por lote, ranking, totais do relatório) estão em Java/JPA. Funcionam, mas ficam invisíveis para quem avalia o banco de dados.
- `GenerateSustainabilityReportUseCase` grava `total_co2_kg = 0` e `tracked_product_count = 0`: o relatório é salvo sem consolidar as emissões do período.
- O "Total de lotes" do relatório é recontado ao vivo a cada leitura, por `produced_at` (uma query por relatório na listagem). O CO₂ é gravado e conta por `calculated_at`. Resultado: o mesmo relatório mistura foto com dado ao vivo, e o total de lotes do PDF pode não bater com a tabela "Emissões por lote".
- Certificações vencidas continuam `ACTIVE` até alguém mudar o status manualmente e seguem somando bônus no score.
- O ranking de sustentabilidade faz N+1: para cada fornecedor busca certificações e emissões, calcula o score em Java (`SustainabilityScoreCalculator`), ordena e pagina em memória com `subList`.
- A "etapa mais recente do lote" (`CROSS JOIN LATERAL ... ORDER BY started_at DESC LIMIT 1`) está copiada em duas queries do `BatchJpaRepository` (contagem de lotes ativos e lotes recentes do dashboard).

## 2. Objetivo

Levar para o banco, como views e procedures, os cálculos que alimentam relatórios, recomendação e gráficos, e fazer a API consumi-los. As views passam a ser as "consultas de relatório" da entrega.

## 3. O que são

- **View:** um `SELECT` salvo com nome, consultado como tabela. Não guarda dados; é recalculada a cada consulta.
- **Stored procedure:** rotina com parâmetros guardada no banco, que pode ter lógica e escrever dados dentro de uma transação.

## 4. Objetos

| Objeto | Tipo | O que entrega | Quem consome |
|---|---|---|---|
| `vw_stage_emission` | View | Uma linha por etapa com emissão: lote, fornecedor, produto, categoria, unidade, etapa, CO₂, fator, metodologia, data do cálculo. | Base das outras views; detalhe do relatório em PDF. |
| `vw_supplier_product_emission` | View | Por fornecedor e produto: CO₂ total, quantidade total, nº de lotes e CO₂ médio por unidade. | [Recomendação de fornecedores](../001-recomendacao-fornecedores/spec.md). |
| `vw_monthly_emission` | View | CO₂ por fornecedor e mês. | [Gráfico de emissões](../003-grafico-emissoes/spec.md) e card "Emissão Total (mês)". |
| `vw_supplier_sustainability` | View | Por fornecedor: certificações ativas, CO₂ total e score de sustentabilidade (RN-05). | Ranking `GET /suppliers/ranking`. |
| `vw_batch_current_stage` | View | Por lote: a etapa mais recente (`chain_id`, `stage_type`, `started_at`). | Dashboard: `activeBatches` e `recentBatches`. |
| `sp_generate_sustainability_report` | Procedure | Consolida CO₂, lotes e produtos rastreados do fornecedor no período e grava o `report`. Devolve o `report_id`. | `POST /suppliers/{id}/reports`. |
| `sp_expire_certifications` | Procedure | Marca como `EXPIRED` as certificações `ACTIVE` com `expires_at` no passado. Devolve quantas mudou. | Job diário do `pg_cron`, dentro do próprio banco. |

## 5. Regras

| ID | Regra |
|---|---|
| RN-01 | O período de emissão é sempre `carbon_emission.calculated_at`, igual ao card do dashboard, para que relatório, gráfico e card batam. |
| RN-02 | CO₂ médio por unidade = soma do CO₂ dos lotes ÷ soma das quantidades dos lotes. Lotes sem nenhuma etapa com emissão calculada ficam fora da média. |
| RN-03 | Só se compara CO₂ por unidade dentro da mesma unidade de medida (`product.unit`). |
| RN-04 | As alterações feitas por procedure continuam auditadas pelos triggers de `audit_log`; o job do `pg_cron` não define `app.user_id`, então a expiração fica no `audit_log` com `user_id` nulo (ação do sistema). |
| RN-05 | Score = `100 + 10 × certificações ACTIVE − CO₂ total ÷ 100` (divisão arredondada para 2 casas), nunca abaixo de 0, com 2 casas. É a mesma fórmula do `SustainabilityScoreCalculator`, que deixa de existir: a fórmula passa a viver só na view. |
| RN-07 | O relatório é um retrato do momento em que foi gerado: CO₂ total, nº de lotes (`batch_count`) e nº de produtos diferentes (`tracked_product_count`) são gravados juntos, com o critério da RN-01, e não mudam depois. Se o período receber dados novos, gera-se outro relatório. O frontend avisa isso na geração e no card. Exemplo: lotes 1 e 17 de café + 1 de milho → 3 lotes, 2 produtos. No negócio real é comum vários lotes do mesmo produto num período (uma cooperativa com um lote de café por semana: 12 lotes, 1 produto). Nos seeds os dois números coincidem nos 100 relatórios, porque nenhum fornecedor repete produto dentro do período; na apresentação a diferença é explicada, sem seed novo. |
| RN-08 | Não se cadastra certificação com `expires_at` no passado: `POST` responde 400. |
| RN-09 | Certificação com `expires_at` no passado não volta para `ACTIVE`: `PATCH .../status` com `ACTIVE` responde 400. Junto com o `pg_cron`, nenhuma certificação vencida fica `ACTIVE` por mais de um dia. |
| RN-06 | A etapa mais recente de um lote é a de maior `started_at`, com `chain.id` como desempate. Lotes sem etapa não aparecem na view. |

## 6. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | As 5 views e as 2 procedures são criadas por migration Flyway. As procedures são `CREATE PROCEDURE` de verdade (chamadas com `CALL`), não `FUNCTION`, como pede o edital. |
| RF-02 | `RankSuppliersBySustainabilityUseCase` (ranking e recomendação), o dashboard e o relatório leem das views em vez de agregar em Java. O ranking filtra, ordena e pagina no banco (`ORDER BY` + `LIMIT/OFFSET`). |
| RF-03 | `GenerateSustainabilityReportUseCase` chama `sp_generate_sustainability_report` e devolve o relatório com os totais preenchidos. |
| RF-04 | O `pg_cron` chama `CALL sp_expire_certifications()` todo dia às 03:00 UTC (00:00 em São Paulo). A expiração não depende da API estar no ar. |
| RF-06 | O agendamento fica numa migration separada da procedure: se um ambiente não tiver `pg_cron`, a procedure continua existindo e pode ser chamada de outro jeito. |
| RF-05 | `docs/sql/consultas-relatorio.sql` reúne consultas de exemplo sobre as views, prontas para a apresentação da A3. |
| RNF-01 | PostgreSQL. O edital pede MySQL, mas a professora liberou o PostgreSQL para este projeto; não há porte para MySQL. |
| RNF-02 | O ranking resolve score e CO₂ de todos os fornecedores da página em uma consulta, sem consulta por fornecedor. |

## 7. Fora de escopo

- Views materializadas (o volume atual não justifica).
- Plano de backup, recuperação e monitoramento (adiado).
