# Especificação — Gestão de produtos no pedido de lote

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/features/014-gestao-produtos/`. Depende da `014-busca-full-text`.

## 1. Problema

- O mercado pede um lote em 4 passos: categoria + unidade (enums) → fornecedor (ranking por CO₂, RN-21 da feature 010) → produto **desse** fornecedor → quantidade. O produto, que é o que o mercado quer, só aparece no 3º passo. Às vezes o passo 3 vem vazio ("este fornecedor não tem produtos nessa categoria").
- O mercado não tem como saber o que costuma pedir: a lista não leva em conta o histórico dele.
- O mercado não acha um produto pelo nome do fornecedor, nem um fornecedor pelo nome do produto.

## 2. Objetivo

O pedido começa pela lista de produtos. Primeiro vêm os que o mercado mais pediu (e teve aceitos) nos últimos 30 dias, depois os de menor CO₂ por unidade. A lista tem busca e filtro opcional de categoria/unidade. Escolher o produto já define o fornecedor.

## 3. Modelo (sem mudança)

O produto continua de **um** fornecedor (RN-16): é ele quem cadastra e edita, na tela de produtos dele. O mesmo produto pode existir em vários fornecedores como cadastros separados: "Café Arábica Orgânico" de A e de B são duas linhas, e o mercado vê as duas, cada uma com o seu fornecedor. Não há catálogo global nem vínculo N:N.

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN-01 | Para o mercado, `GET /products` lista os produtos de todos os fornecedores (catálogo, RN-13). Cada item traz `supplierName`, `co2KgPerUnit` e `acceptedBatchesLast30Days`. |
| RN-02 | Ordem: `acceptedBatchesLast30Days` decrescente → `co2KgPerUnit` crescente, com `null` (sem histórico) no fim → `name` → `productId`. |
| RN-03 | `acceptedBatchesLast30Days` conta os lotes **deste mercado** daquele produto com status `ACTIVE` e `created_at` nos últimos 30 dias. Como a data do aceite não é gravada, vale a data do pedido. Para o `PLATFORM_ADMIN`, conta os de todos os mercados. |
| RN-04 | `co2KgPerUnit` é o CO₂ médio por unidade do **fornecedor** na categoria e unidade do produto, o mesmo número da recomendação RN-21. Sem histórico, vem `null`. |
| RN-05 | `search` casa nome e descrição do produto **ou** nome do fornecedor, com a semântica da feature 014 (prefixo, sem acento). Cada lado é avaliado inteiro: "café serra" não acha o café do fornecedor "Grãos da Serra", porque as duas palavras precisam estar no produto ou as duas no fornecedor. A busca pelo rótulo da categoria ("Têxtil") continua. |
| RN-06 | `category` e `unit` são filtros opcionais e independentes. |
| RN-07 | Para a companhia fornecedora, `GET /products` continua como hoje: só os produtos dela, por `productId`, sem os campos da RN-01 (eles vêm `null`). |
| RN-08 | O pedido (`POST /batches`) não muda: `productId` + `supplierId` do produto escolhido + `quantity`. |
| RN-09 | A recomendação por `category`+`unit` em `GET /suppliers?ranked=true` sai da API. Ela passa para a ordem da RN-02. O ranking por sustentabilidade, sem esses parâmetros, continua. |

## 5. Contrato

`GET /api/v1/products?limit=20&offset=0&search={termo}&category={c}&unit={u}`

Item (mercado):

```json
{
  "productId": 12, "supplierId": 4, "name": "Café Arábica Orgânico",
  "category": "AGRICULTURE", "unit": "KG", "description": "...",
  "supplierName": "Produtores de Café Sustentável do Interior",
  "co2KgPerUnit": 0.8421,
  "acceptedBatchesLast30Days": 3
}
```

`supplierId` continua aceito, mas o front deixa de usá-lo no pedido de lote. Depois da Task 2, `GET /suppliers?ranked=true&category=...&unit=...` responde `400` (parâmetro desconhecido) ou ignora os parâmetros, conforme o padrão do controller.

## 6. Fora do escopo

- Catálogo global, vínculo N:N e deduplicação de produtos entre fornecedores.
- Gravar a data de aceite do lote (`accepted_at`). Ela entra se a janela da RN-03 tiver que ser pela data do aceite.
- Preço, estoque ou disponibilidade por fornecedor.
- Busca cruzando produto e fornecedor na mesma query ("café serra").

## 7. Critérios de aceite

- Um mercado com 2 lotes `ACTIVE` de "Arroz" nos últimos 30 dias vê "Arroz" antes dos produtos sem lote. Um lote `PENDING` ou `REJECTED`, ou um lote de outro mercado, não muda essa ordem.
- Entre produtos sem lote recente, o de menor `co2KgPerUnit` vem primeiro, e os sem histórico vêm no fim.
- `?search=serra` traz os produtos de "Grãos da Serra Cooperativa". `?search=cafe` traz os cafés de todos os fornecedores.
- `?category=AGRICULTURE&unit=KG` filtra, e cada filtro também funciona sozinho.
- A companhia fornecedora continua vendo só os produtos dela.
- O `./mvnw verify` passa.
