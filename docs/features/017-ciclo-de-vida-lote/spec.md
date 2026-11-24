# Especificação: Ciclo de vida do lote

> A arquitetura está em [`plan.md`](plan.md) e a execução em [`tasks.md`](tasks.md). A contraparte no frontend fica em `supply-chain-verde-web/docs/features/016-ciclo-de-vida-lote/`. Esta spec complementa as RN-17 a RN-20 da `010-empresas-planos`: o lote nasce `PENDING`, a Produção o ativa e a recusa é final.

## 1. Problema

- O lote só tem três status (`PENDING`, `ACTIVE` e `REJECTED`). Quando chega ao Varejo, continua `ACTIVE` para sempre. Nada diz que a jornada acabou, e o filtro "Em andamento" acumula lotes já entregues.
- O mercado não consegue desistir de um pedido. Um lote pedido por engano fica pendente até o fornecedor recusá-lo.
- Depois que o lote está `ACTIVE`, qualquer `ADMIN`, `MANAGER` ou `SUPPLIER` do mercado ou do fornecedor registra qualquer etapa. Com isso, o mercado lança a Produção e a Armazenagem do fornecedor, que são os dados que alimentam o CO₂, e o fornecedor declara o Varejo, que é o recebimento do mercado. Isso enfraquece a rastreabilidade.

## 2. Objetivo

- Formalizar o ciclo de vida do lote como uma máquina de estados com dois status novos, `COMPLETED` e `CANCELED`.
- Cada transição e cada etapa passa a ter um único lado responsável. O mercado decide sobre o pedido e confirma o recebimento. O fornecedor decide se aceita o pedido e registra o que acontece enquanto o lote está com ele.
- Corrigir os dados legados para que eles sigam as regras novas.

## 3. Glossário

| Termo | Significado |
|---|---|
| Lado | O tipo da companhia de quem age (`MARKET` ou `SUPPLIER`), dado pelo token. O `PLATFORM_ADMIN` não tem lado. |
| Dono da etapa | O lado que registra uma etapa: o fornecedor registra de `PRODUCTION` a `DISTRIBUTION` e o mercado registra `RETAIL`. |
| Estado final | Um status que não tem transição de saída: `COMPLETED`, `REJECTED` e `CANCELED`. |

## 4. Regras de negócio

### Máquina de estados

```
           ┌── PRODUCTION (fornecedor) ──► ACTIVE ── RETAIL (mercado) ──► COMPLETED
PENDING ───┼── recusar (fornecedor) ─────► REJECTED
           └── cancelar (mercado) ───────► CANCELED
```

| ID | Regra |
|---|---|
| RN-01 | O lote só muda de status pelas transições acima: `PENDING` vai para `ACTIVE`, `REJECTED` ou `CANCELED`, e `ACTIVE` vai para `COMPLETED`. Qualquer outra transição devolve 409. |
| RN-02 | `COMPLETED`, `REJECTED` e `CANCELED` são finais. Um lote nesses status não recebe etapa nem muda de status (409). |
| RN-03 | O `PLATFORM_ADMIN` executa qualquer transição, mas também obedece à RN-01. Ele não pula estados. |

### Quem faz cada transição

| ID | Regra |
|---|---|
| RN-04 | Só o mercado do lote (`ADMIN` ou `MANAGER`) pede o lote, que nasce `PENDING`. Isso não muda em relação à RN-17 da 010. |
| RN-05 | Só o mercado do lote (`ADMIN` ou `MANAGER`) cancela, e só enquanto o lote está `PENDING`. O cancelamento não tem motivo. Outra companhia, inclusive o fornecedor, recebe 403. |
| RN-06 | Só o fornecedor do lote (`ADMIN` ou `SUPPLIER`) recusa, com motivo obrigatório. Isso não muda em relação à RN-19 da 010. |
| RN-07 | O lote vira `ACTIVE` quando o fornecedor registra a `PRODUCTION` (RN-18 da 010) e vira `COMPLETED` quando o mercado registra o `RETAIL`. As duas mudanças acontecem na mesma transação da etapa. |

### Dono da etapa

| ID | Regra |
|---|---|
| RN-08 | O fornecedor do lote registra `PRODUCTION`, `STORAGE`, `PROCESSING`, `TRANSPORT` e `DISTRIBUTION`. O mercado do lote registra só o `RETAIL`. O lado errado recebe 403. Transporte e Distribuição ficam com o fornecedor, que é quem contrata o frete na maioria dos casos. |
| RN-09 | O responsável da etapa (`X-Responsible-User-Id`) precisa ser da companhia dona da etapa (403). Hoje basta que ele seja do mercado ou do fornecedor do lote. |
| RN-10 | Quem registra a etapa também registra o transporte e calcula a emissão dela. Por isso o `SUPPLIER` passa a poder chamar `POST /stages/{id}/emission`, que hoje é só de `MANAGER` e `ADMIN`. |
| RN-11 | A sequência das etapas continua a mesma (007-ordem-etapas). A RN-08 só decide quem registra a próxima etapa. |

### Leituras que dependem do status

| ID | Regra |
|---|---|
| RN-12 | A recomendação de fornecedores conta os lotes aceitos nos últimos 30 dias como `ACTIVE` ou `COMPLETED` (hoje conta só `ACTIVE`). Concluir um lote não pode tirar pontos do fornecedor. |
| RN-13 | O card "Lotes ativos" do dashboard conta `status = 'ACTIVE'`. Depois da migração, isso dá o mesmo número que a regra atual (a etapa atual é diferente de `RETAIL`). |
| RN-14 | `GET /batches?status=` passa a aceitar `COMPLETED` e `CANCELED`. |

### Dados legados

| ID | Regra |
|---|---|
| RN-15 | Todo lote `ACTIVE` que já tem a etapa `RETAIL` vira `COMPLETED`. |
| RN-16 | Uma etapa existente cujo responsável não é da companhia dona da etapa (RN-08) passa a ter como responsável o usuário ativo de menor id dessa companhia, com perfil permitido: `ADMIN` ou `SUPPLIER` no fornecedor, `ADMIN` ou `MANAGER` no mercado. Se a companhia não tiver um usuário assim, a etapa fica como está. |
| RN-17 | A correção dos dados legados fica na auditoria pelos triggers que já existem em `batch` e `chain`. |

## 5. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `BatchStatus` ganha `COMPLETED` e `CANCELED` e declara as transições válidas (RN-01 e RN-02). |
| RF-02 | `PATCH /batches/{id}/cancel`, sem corpo, só para `MANAGER` e `ADMIN` (o `PLATFORM_ADMIN` herda o `ADMIN`). A resposta é o `BatchResponseDTO` (RN-05). |
| RF-03 | `POST /batches/{id}/stages` valida o dono da etapa (RN-08 e RN-09) e conclui o lote no `RETAIL` (RN-07). |
| RF-04 | `PATCH /batches/{id}/reject` deixa o `PLATFORM_ADMIN` passar (RN-03). Hoje ele recebe 403. |
| RF-05 | `POST /stages/{id}/emission` aceita `SUPPLIER` (RN-10). |
| RF-06 | A migration `V20` altera o enum e a check de `produced_at` e corrige os dados legados (RN-15 e RN-16). |
| RNF-01 | Erros no formato atual: 403 (lado errado, outra companhia, responsável de outra companhia), 404 (lote inexistente) e 409 (transição inválida, lote em estado final). |
| RNF-02 | A máquina de estados fica no domínio, sem dependência nova. |

## 6. Contratos

### `PATCH /api/v1/batches/{batchId}/cancel`

Não tem corpo. A resposta é `200` com o lote:

```json
{ "batchId": 258, "status": "CANCELED", "rejectionReason": null, "...": "campos atuais" }
```

Se o lote não estiver mais `PENDING`, a resposta é `409`: `{ "message": "O lote 258 está ACTIVE e não aceita esta ação." }` (a mensagem atual da `BatchStatusConflictException`).

### `POST /api/v1/batches/{batchId}/stages`

O contrato não muda. O que muda:

- Lado errado: `403`, com a mensagem `"A etapa RETAIL é registrada pelo mercado do lote."` ou `"A etapa STORAGE é registrada pelo fornecedor do lote."`.
- Com `stageType: "RETAIL"`, o lote vai para `COMPLETED`. O `GET /batches` seguinte já traz `status: "COMPLETED"`.

### `GET /api/v1/batches`

`status` e o `status` de cada item ganham `COMPLETED` e `CANCELED`.

## 7. Fora do escopo

- Cancelar depois do aceite. Quando o fornecedor já está produzindo, o cancelamento é uma negociação fora do sistema.
- Motivo de cancelamento.
- Um terceiro ator (transportadora ou distribuidor) que registre Transporte ou Distribuição.
- Desfazer uma etapa ou reabrir um lote em estado final.
- Spring Statemachine ou outra biblioteca de máquina de estados: 5 estados e 4 transições cabem numa tabela no enum.

## 8. Critérios de aceite

- `PATCH /batches/{pending}/cancel` como `MANAGER` do mercado devolve 200 com `CANCELED`. Como `ADMIN` do fornecedor, devolve 403. Em um lote `ACTIVE`, devolve 409.
- `POST /batches/{active}/stages` com `STORAGE`, como `MANAGER` do mercado, devolve 403. Como `SUPPLIER` do fornecedor, devolve 201.
- `POST /batches/{distribuído}/stages` com `RETAIL`, como `SUPPLIER`, devolve 403. Como `MANAGER` do mercado, devolve 201, e o lote fica `COMPLETED`.
- Um lote `COMPLETED`, `REJECTED` ou `CANCELED` devolve 409 a qualquer etapa, recusa ou cancelamento, inclusive para o `PLATFORM_ADMIN`.
- Um `SUPPLIER` calcula a emissão da etapa que registrou (200).
- Depois da `V20`, nenhum lote `ACTIVE` tem `RETAIL`, e nenhuma etapa de fornecedor tem como responsável um usuário de mercado (e vice-versa), quando a companhia dona tem um usuário elegível.
- O `./mvnw verify` passa.
