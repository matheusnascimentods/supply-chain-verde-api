# Especificação — Views e procedures

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Atende ao entregável #4 da A3 (views, stored procedures e consultas para relatórios em SQL).

## 1. Problema

- Os cálculos de sustentabilidade (CO₂ por lote, ranking, totais do relatório) estão em Java/JPA. Funcionam, mas ficam invisíveis para quem avalia o banco de dados.
- `GenerateSustainabilityReportUseCase` grava `total_co2_kg = 0` e `tracked_product_count = 0`: o relatório é salvo sem consolidar as emissões do período.
- Certificações vencidas continuam `ACTIVE` até alguém mudar o status manualmente.

## 2. Objetivo

Levar para o banco, como views e procedures, os cálculos que alimentam relatórios, recomendação e gráficos, e fazer a API consumi-los. As views passam a ser as "consultas de relatório" da entrega.

## 3. O que são

- **View:** um `SELECT` salvo com nome, consultado como tabela. Não guarda dados; é recalculada a cada consulta.
- **Stored procedure:** rotina com parâmetros guardada no banco, que pode ter lógica e escrever dados dentro de uma transação.

## 4. Objetos

| Objeto | Tipo | O que entrega | Quem consome |
|---|---|---|---|
| `vw_stage_emission` | View | Uma linha por etapa com emissão: lote, fornecedor, produto, categoria, unidade, etapa, CO₂, fator, metodologia, data do cálculo. | Base das outras views; detalhe do relatório em PDF. |
| `vw_supplier_product_emission` | View | Por fornecedor e produto: CO₂ total, quantidade total, nº de lotes e CO₂ médio por unidade. | [Recomendação de fornecedores](../recomendacao-fornecedores/spec.md). |
| `vw_monthly_emission` | View | CO₂ por fornecedor e mês. | [Gráfico de emissões](../grafico-emissoes/spec.md) e card "Emissão Total (mês)". |
| `sp_generate_sustainability_report` | Procedure | Consolida CO₂ e produtos rastreados do fornecedor no período e grava o `report`. Devolve o `report_id`. | `POST /suppliers/{id}/reports`. |
| `sp_expire_certifications` | Procedure | Marca como `EXPIRED` as certificações `ACTIVE` com `expires_at` no passado. Devolve quantas mudou. | Job diário da API. |

## 5. Regras

| ID | Regra |
|---|---|
| RN-01 | O período de emissão é sempre `carbon_emission.calculated_at`, igual ao card do dashboard, para que relatório, gráfico e card batam. |
| RN-02 | CO₂ médio por unidade = soma do CO₂ dos lotes ÷ soma das quantidades dos lotes. Lotes sem nenhuma etapa com emissão calculada ficam fora da média. |
| RN-03 | Só se compara CO₂ por unidade dentro da mesma unidade de medida (`product.unit`). |
| RN-04 | As alterações feitas por procedure continuam auditadas pelos triggers de `audit_log`; o job de expiração grava com a identidade de serviço definida em `docs/spec.md`. |

## 6. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | As 3 views e as 2 procedures são criadas por migration Flyway. |
| RF-02 | `RankSuppliersBySustainabilityUseCase` (recomendação), o dashboard e o relatório leem das views em vez de agregar em Java. |
| RF-03 | `GenerateSustainabilityReportUseCase` chama `sp_generate_sustainability_report` e devolve o relatório com os totais preenchidos. |
| RF-04 | Um `@Scheduled` diário chama `sp_expire_certifications`. |
| RF-05 | `docs/sql/consultas-relatorio.sql` reúne consultas de exemplo sobre as views, prontas para a apresentação da A3. |
| RNF-01 | SQL o mais portável possível. Onde a sintaxe for exclusiva do PostgreSQL (ex.: `date_trunc`, `LANGUAGE plpgsql`), o plan registra o equivalente em MySQL, já que a troca de SGBD está em negociação. |

## 7. Fora de escopo

- Views materializadas (o volume atual não justifica).
- Plano de backup, recuperação e monitoramento (adiado).
