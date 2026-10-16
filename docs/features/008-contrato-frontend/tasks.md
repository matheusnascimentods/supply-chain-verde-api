# Tarefas — Impactos de contrato no frontend

> Mudanças da [006](../006-views-procedures/spec.md) e da [007](../007-ordem-etapas/spec.md) que alteram o que a API aceita ou devolve, e o que o `supply-chain-verde-web` precisa ajustar. Nenhum campo é renomeado nem removido; mudam regras, erros e o significado de valores.

**Status:** não iniciado. Cada item do web entra junto ou depois da task da API correspondente.

O web já mostra a mensagem da API para 400/409 (`shared/utils/http/index.utils.ts`), então as validações novas aparecem para o usuário mesmo antes do ajuste de tela. Os itens abaixo evitam que ele chegue a ver o erro.

---

## Task 1 — certificações (006, RN-08 e RN-09)

| Contrato | Antes | Depois |
|---|---|---|
| `POST /suppliers/{id}/certifications` com `expiresAt` no passado | 201, grava `ACTIVE` | 400 |
| `PATCH /certifications/{id}/status` com `ACTIVE` e `expiresAt` no passado | 200 | 400 |
| `status` de certificação vencida | fica `ACTIVE` até alguém mudar | vira `EXPIRED` na madrugada seguinte (`pg_cron`), sem ação de usuário |

- [ ] `certifications/presentation/components/form`: validator e `min` (hoje) em `expiresAt`. Hoje só tem `Validators.required`
- [ ] `suppliers/presentation/components/certifications-modal`: desabilitar "Ativa" no menu de status quando `expiresAt` já passou (o menu lista todos os `CERTIFICATION_STATUSES`)

## Task 2 — relatórios (006, RN-07)

| Contrato | Antes | Depois |
|---|---|---|
| `totalCo2Kg` de relatório gerado pela API | sempre `0` | total do período |
| `totalBatchCount` | recontado a cada leitura, lotes por `produced_at` | gravado na geração, lotes com emissão calculada no período (bate com a tabela "Emissões por lote" do PDF) |
| `trackedProductCount` | sempre `0` | produtos diferentes do período |
| Relatórios já existentes | — | recalculados uma vez pela migration (17 dos 100 do banco local ficam em 0 por não ter emissão no período) |

- [ ] `suppliers/presentation/components/reports-modal`: aviso no formulário de geração e no card, "Retrato da data de geração. Dados lançados depois exigem gerar um novo relatório."
- [ ] PDF (`report-pdf`): mesma frase perto da data de geração
- [ ] Ajustar fixtures de teste que assumem `trackedProductCount` nulo ou `0`

## Task 3 — etapas (007)

| Contrato | Antes | Depois |
|---|---|---|
| `POST /batches/{batchId}/stages` fora da sequência `PRODUCTION → STORAGE → PROCESSING → TRANSPORT → DISTRIBUTION → RETAIL`, ou repetida | 201 | 400 |
| Etapas dos lotes existentes | como cadastradas | a migration insere as que faltavam (82 no banco local, quase todas `STORAGE`), sem endereço, transporte nem emissão |
| `currentStage` / `status` no dashboard | — | sem mudança: as etapas inseridas ficam sempre antes da atual |

- [ ] `batches/presentation/components/stage-modal`: oferecer só a próxima etapa do lote (hoje o `select` lista os 6 `STAGE_TYPES`) e esconder "adicionar etapa" depois de `RETAIL`
- [ ] Rastreabilidade (`traceability`) e `batch-card`: conferir que etapa sem endereço, sem transporte e sem emissão renderiza sem quebrar

## Sem impacto no web

- Ranking (`vw_supplier_sustainability`): mesma resposta, ordem e paginação; o aceite da 006 Task 4 é o diff igual.
- Dashboard (`vw_batch_current_stage`): mesmos números.
