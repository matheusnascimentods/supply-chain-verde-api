# Especificação — Padronização do schema

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Diagrama do schema final em [`schema.puml`](schema.puml).

## 1. Problema

- Cada tabela nomeia a PK de um jeito (`supplier_id`, `log_id`, `emission_id`...).
- A data de criação tem quatro nomes (`registered_at`, `generated_at`, `performed_at`, `calculated_at`), dois tipos (`DATE` e `TIMESTAMP` sem fuso) e falta em 7 tabelas. Nenhuma tabela tem data de alteração.
- `supplier.name_search`, `supplier.cnpj_digits` e `product.search_vector` guardam como coluna o que é só índice de busca. Elas aparecem nas entidades e precisaram ser tiradas da auditoria à mão (`V30`).

## 2. Objetivo

Padronizar o schema sem alterar o contrato de nenhuma rota: mesmas rotas, parâmetros, campos, tipos e formatos de data.

## 3. Regras

| ID | Regra |
|---|---|
| RN-01 | A PK de toda tabela se chama `id`. As FKs continuam `<tabela>_id` (`batch.supplier_id → supplier.id`). |
| RN-02 | Toda tabela tem `created_at` e `updated_at` `TIMESTAMPTZ NOT NULL DEFAULT now()`. `registered_at`, `generated_at`, `performed_at` e `calculated_at` viram `created_at`. Nas tabelas que não tinham a coluna, as linhas antigas recebem a data de negócio quando ela existe (`issued_at`, `produced_at`, `started_at`). Sem ela, recebem o instante da migration. |
| RN-03 | `updated_at` é mantido por trigger no Postgres e só muda quando algum valor da linha muda. Na inserção, é igual ao `created_at`. |
| RN-04 | `audit_log` é append-only: tem `created_at` e não tem `updated_at`. |
| RN-05 | O fuso de negócio é `America/Sao_Paulo`: datas da API, dia da emissão e mês das views. |
| RN-06 | A busca textual de fornecedor e produto usa índices de expressão, não colunas. |
| RN-07 | O histórico de `audit_log` é reescrito para os nomes novos. Assim, `beforeData`/`afterData` antigos e novos usam as mesmas chaves. |
| RN-08 | `updated_at` entra nos snapshots de auditoria como qualquer outra coluna. |
| RN-09 | O schema assume PostgreSQL. Equivalentes em MySQL ficam só como nota no plan. |

## 4. Contrato preservado

Nenhum campo é criado ou removido nas respostas, e `updatedAt` não é exposto. Os campos que mudam de origem:

| Campo na API | Tipo na API | Origem antes | Origem depois |
|---|---|---|---|
| `supplierId`, `reportId`, `emissionId`, `logId`, `userId`... | número | `<tabela>_id` | `id` |
| `registeredAt` (fornecedor, ranking) | `LocalDate` | `supplier.registered_at` | dia de `supplier.created_at` em São Paulo |
| `calculatedAt` (emissão) | `LocalDate` | `carbon_emission.calculated_at` | dia de `carbon_emission.created_at` em São Paulo |
| `generatedAt` (relatório) | `LocalDateTime` | `report.generated_at` | `report.created_at` |
| `performedAt` (auditoria) | `LocalDateTime` | `audit_log.performed_at` | `audit_log.created_at` |
| `createdAt` (usuário) | `LocalDateTime` | `users.created_at` | `users.created_at` (agora `TIMESTAMPTZ`) |

Os filtros por período continuam com o mesmo comportamento: relatório e emissões pelo dia em São Paulo, auditoria por `from`/`to`.

## 5. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `V34` troca as colunas de busca por índices de expressão. |
| RF-02 | `V35` aplica RN-01 a RN-04 e RN-07 sobre os dados existentes, recria triggers de auditoria e views. |
| RF-03 | A JVM roda em `America/Sao_Paulo`. |
| RF-04 | Entidades JPA, queries nativas e testes passam a usar os nomes novos. Os nomes dos campos Java ficam iguais. |
| RNF-01 | O `/v3/api-docs` gerado antes e depois da mudança é idêntico. |
| RNF-02 | As asserções de corpo de resposta dos testes de integração existentes não mudam. Só mudam os SQLs de preparação. |

## 6. Fora de escopo

- Expor `updatedAt` na API.
- Renomear sequências (`supplier_supplier_id_seq`) e constraints (`fk_batch_supplier`, `supplier_pkey`). Elas seguem funcionando com os nomes antigos.
- Editar as migrations já aplicadas (`V1`–`V33`).
