# Especificação — Recomendação de fornecedores sustentáveis

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/recomendacao-fornecedores/`.

## 1. Problema

O tema da A3 pede "recomendação de fornecedores sustentáveis". Hoje existe só um ranking global por `sustainabilityScore`, que não considera o produto: quem cria um lote de café não sabe qual fornecedor produz café com menos emissão.

## 2. Objetivo

A busca de fornecedores aceita, opcionalmente, o produto do lote e devolve os fornecedores do melhor para o pior naquele produto. O frontend destaca o primeiro como recomendado.

## 3. Regras

| ID | Regra |
|---|---|
| RN-01 | "Melhor" = menor CO₂ médio por unidade do produto (ver [views-procedures](../views-procedures/spec.md), RN-02). |
| RN-02 | Produto já cadastrado: compara pelos lotes daquele `productId`. |
| RN-03 | Produto novo (ainda sem id): compara pelos lotes de produtos da mesma `category` **e** mesma `unit`. Unidades diferentes nunca se comparam. |
| RN-04 | Empate no CO₂ por unidade: maior `sustainabilityScore` primeiro, depois `supplierId`. |
| RN-05 | Fornecedores sem lote com emissão naquele produto/categoria+unidade aparecem no fim, com `co2KgPerUnit = null`, ordenados por `sustainabilityScore`. |
| RN-06 | Sem os parâmetros novos, o comportamento atual do ranking não muda. |

## 4. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `GET /suppliers?ranked=true` aceita `productId` **ou** o par `category` + `unit`. |
| RF-02 | `productId` junto com `category`/`unit` retorna 400. `category` sem `unit` (ou o contrário) retorna 400. `productId` inexistente retorna 404. |
| RF-03 | Cada item do ranking ganha `co2KgPerUnit` (decimal ou `null`); só vem preenchido quando a busca usa os parâmetros novos. |
| RF-04 | Paginação (`limit`, `offset`) e `search` continuam funcionando; a ordem vale para a lista inteira, não por página. |
| RNF-01 | A ordenação é feita no banco, a partir de `vw_supplier_product_emission`. |

## 5. Contrato

```http
GET /api/v1/suppliers?ranked=true&productId=12&limit=20&offset=0
GET /api/v1/suppliers?ranked=true&category=AGRICULTURE&unit=KG&limit=20&offset=0
```

```json
{
  "items": [
    { "supplierId": 3, "name": "Fazenda Verde", "co2KgPerUnit": 0.4210, "sustainabilityScore": 87.5, "...": "demais campos atuais" },
    { "supplierId": 8, "name": "Sítio Boa Terra", "co2KgPerUnit": 0.6034, "sustainabilityScore": 91.0 },
    { "supplierId": 5, "name": "Agro Novo", "co2KgPerUnit": null, "sustainabilityScore": 70.0 }
  ],
  "limit": 20, "offset": 0, "hasNext": false, "totalPages": 1
}
```

## 6. Fora de escopo

- Pesos combinando CO₂, certificações e score.
- Recomendação por etapa da cadeia (ex.: melhor transportadora).
