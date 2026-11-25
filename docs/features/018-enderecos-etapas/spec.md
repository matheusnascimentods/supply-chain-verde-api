# Especificação: Endereços das etapas do lote

> A arquitetura está em [`plan.md`](plan.md) e a execução em [`tasks.md`](tasks.md). A contraparte no frontend fica em `supply-chain-verde-web/docs/features/017-enderecos-etapas/`. Esta spec resolve a pendência de endereço marcada no `Chain` ("ver pendência na seção 13") e complementa a RN-08 da `017-ciclo-de-vida-lote`, que define o dono de cada etapa.

## 1. Problema

- Um endereço só existe se for o endereço de uma companhia. A etapa (`chain`) apenas aponta para um deles por `origin_address_id` e `destination_address_id`, e não há como cadastrar um armazém, uma fábrica ou um centro de distribuição.
- O front oferece ao fornecedor um único endereço possível, o dele. Por isso todo lote termina "no fornecedor" ou sem destino, e o card mostra sempre o mesmo destino.
- Origem e destino são informados de forma independente. Nada garante que a origem de uma etapa seja o lugar onde a etapa anterior terminou, então a rota pode ter saltos.
- `RegisterChainStageUseCase` aceita qualquer `addressId`, inclusive de outra companhia, e troca um id inexistente por `null` sem avisar.

## 2. Objetivo

- O dono da etapa informa **só o destino**, como um endereço completo e novo. A origem passa a ser derivada da cadeia.
- O Varejo termina no endereço do mercado do lote, sem escolha.
- Remover `chain.origin_address_id`. A origem deixa de ser um dado gravado e passa a ser uma regra.

## 3. Glossário

| Termo | Significado |
|---|---|
| Destino | O local onde o lote está ao fim da etapa. É gravado em `chain.destination_address_id`. |
| Origem | O local onde o lote está no início da etapa. É derivada (RN-04) e nunca é gravada. |
| Etapa anterior | A etapa do mesmo lote com o `stage_type` imediatamente anterior na sequência da `007-ordem-etapas`. |

## 4. Regras de negócio

### Destino

| ID | Regra |
|---|---|
| RN-01 | Toda etapa de `PRODUCTION` a `DISTRIBUTION` exige o destino como endereço completo: `street`, `number`, `neighborhood`, `zipCode`, `city` e `state` obrigatórios, e `complement` opcional. Se faltar algum, a resposta é 400. Quando o lote não sai do lugar, o destino é o mesmo local da origem, digitado de novo. |
| RN-02 | Cada etapa cria uma linha nova em `address` para o seu destino. A API não aceita mais id de endereço no request, então não há como apontar para o endereço de outra companhia. |
| RN-03 | O destino do `RETAIL` é o endereço atual da companhia do mercado do lote (`company.address_id`), definido pela API. Um `RETAIL` enviado com `destination` recebe 400. |

### Origem

| ID | Regra |
|---|---|
| RN-04 | A origem da `PRODUCTION` é o endereço atual da companhia fornecedora do lote. A origem de cada etapa seguinte é o destino da etapa anterior. |
| RN-05 | A origem e o destino do `RETAIL` apontam para a companhia e seguem o cadastro atual dela. Se o fornecedor ou o mercado mudar de endereço, lotes antigos passam a mostrar o endereço novo. Isso é aceito, e não há snapshot. |
| RN-06 | A API continua devolvendo `originAddress` em toda leitura de etapa: no response do `POST`, no `GET /batches/{id}/stages` e nas etapas embutidas em `GET /batches`. O valor é calculado pela RN-04. |

### Dados legados

| ID | Regra |
|---|---|
| RN-07 | Antes de remover a coluna, toda etapa com `destination_address_id` nulo recebe `origin_address_id` da própria etapa (o lote ficou no lugar). Se esse valor também for nulo, recebe o endereço da companhia fornecedora do lote. |
| RN-08 | Toda etapa `RETAIL` passa a apontar para o endereço da companhia do mercado do lote (RN-03). |
| RN-09 | Depois do backfill, `chain.destination_address_id` passa a ser `NOT NULL`, e `origin_address_id` (coluna, FK e índice) é removida. Os triggers de auditoria de `chain` deixam de citar a coluna. |

## 5. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `ChainRequestDTO` troca `originAddressId` e `destinationAddressId` por `destination: AddressRequestDTO` (RN-01 a RN-03). |
| RF-02 | `RegisterChainStageUseCase` valida e salva o destino, ou usa o endereço do mercado no `RETAIL`. |
| RF-03 | Toda leitura de etapa monta `originAddress` pela RN-04 (RN-06). |
| RF-04 | A migration `V21` faz o backfill (RN-07, RN-08), aplica `NOT NULL`, remove a coluna e recria os triggers de `chain` (RN-09). |
| RNF-01 | Erros no formato atual: 400 (destino ausente ou incompleto, destino enviado no `RETAIL`). As regras de 403, 404 e 409 da 017 não mudam. |
| RNF-02 | Não há endpoint novo de endereço nem tabela nova. |

## 6. Contratos

### `POST /api/v1/batches/{batchId}/stages`

Request, para etapas do fornecedor:

```json
{
  "stageType": "STORAGE",
  "startedAt": "2026-11-25T08:00:00",
  "endedAt": null,
  "destination": {
    "street": "Rod. Anhanguera", "number": "km 102", "neighborhood": "Distrito Industrial",
    "complement": "Galpão 3", "zipCode": "13069-000", "city": "Campinas", "state": "SP"
  }
}
```

No `RETAIL`, `destination` fica ausente ou `null`. `originAddressId` e `destinationAddressId` deixam de existir. O Jackson ignora esses campos se vierem, e nesse caso a falta de `destination` gera 400.

Response `201`: o formato não muda (`ChainResponseDTO`). `originAddress` vem derivado e `destinationAddress` sempre preenchido.

Erros novos:

- `400 { "message": "Destino da etapa é obrigatório." }`
- `400 { "message": "Destino incompleto: informe logradouro, número, bairro, CEP, cidade e UF." }`
- `400 { "message": "O destino do varejo é o endereço do mercado e não pode ser informado." }`

### Leituras

`GET /batches/{id}/stages` e `GET /batches` mantêm o contrato. Só a origem passa a ser calculada.

## 7. Fora do escopo

- Snapshot do endereço da companhia na etapa (RN-05).
- Sugerir ou reaproveitar endereços já usados, ou deduplicar linhas de `address`.
- Calcular a distância do transporte a partir dos endereços.
- Editar o destino de uma etapa já registrada.
- Validar o CEP na API (o front já consulta o ViaCEP).

## 8. Critérios de aceite

- `POST` de `PRODUCTION` sem `destination` devolve 400. Com destino completo devolve 201, com `originAddress` igual ao endereço do fornecedor e `destinationAddress` igual ao enviado, com id novo.
- `POST` de `STORAGE` devolve `originAddress` igual ao `destinationAddress` da `PRODUCTION`.
- `POST` de `RETAIL` com `destination` devolve 400. Sem `destination` devolve 201, com `destinationAddress` igual ao endereço do mercado e `originAddress` igual ao destino da `DISTRIBUTION`.
- Depois da `V21`: `chain` não tem `origin_address_id`, nenhuma etapa tem destino nulo e todo `RETAIL` aponta para o endereço do mercado. Os triggers de auditoria de `chain` funcionam em insert, update e delete.
- O `./mvnw verify` passa.
