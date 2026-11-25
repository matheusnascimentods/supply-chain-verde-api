# Especificação: Cálculo de carbono

> A arquitetura está em [`plan.md`](plan.md) e a execução em [`tasks.md`](tasks.md). Os problemas de origem estão em [`debitos.md`](debitos.md) (citados como D-n). A contraparte no frontend fica em `supply-chain-verde-web/docs/features/018-calculo-de-carbono/`. Esta spec substitui a regra de cálculo da `000-base` (§3.4) e complementa a RN-10 da `017-ciclo-de-vida-lote`.

## 1. Problema

- O CO₂ de toda etapa é `distância × 0,15`. Só a etapa `TRANSPORT` tem distância, então Produção, Armazenagem, Processamento, Distribuição e Varejo valem 0 (D-1, D-4).
- Metodologia, modo de transporte e combustível são gravados, mas não mudam o número (D-2, D-3).
- O cálculo é opcional e não pode ser feito depois, então há etapas sem emissão que somam 0 sem ninguém perceber (D-5, D-6).
- Calcular a mesma etapa duas vezes devolve 500 (D-7).
- O score do ranking desconta o CO₂ absoluto e pune o fornecedor pelo volume, não pela eficiência (D-10).

## 2. Objetivo

- Calcular a emissão de cada etapa a partir do dado da atividade que a gera, com o fator da metodologia escolhida.
- Tornar a emissão obrigatória: uma etapa sem emissão impede a próxima.
- Pontuar o fornecedor pela intensidade de carbono (kg CO₂e por tonelada entregue).

## 3. Glossário

| Termo | Significado |
|---|---|
| CO₂e | CO₂ equivalente: CO₂, CH₄ e N₂O convertidos pelo potencial de aquecimento global. É a unidade de `co2_kg` a partir desta feature (D-8). O nome da coluna não muda. A exceção é a eletricidade: o MCTI publica o fator do SIN só em CO₂, e é esse valor que entra na conta. |
| Etapa de deslocamento | `TRANSPORT` e `DISTRIBUTION`. Emitem pelo frete. |
| Etapa fixa | `PRODUCTION`, `STORAGE`, `PROCESSING` e `RETAIL`. Emitem pelo consumo de energia ou combustível no local. |
| Fator | kg CO₂e por unidade de atividade (tonelada-km, kWh, litro, kg ou m³), de uma metodologia e de um ano de publicação. |
| Intensidade | kg CO₂e total dividido pelas toneladas entregues. |

## 4. Regras de negócio

### Metodologias e fatores

| ID | Regra |
|---|---|
| RN-01 | As metodologias aceitas são `GHG_PROTOCOL` e `DEFRA`. `IPCC` e `EMEP_EEA` deixam de existir. |
| RN-02 | Os fatores ficam na tabela `emission_factor`, com metodologia, atividade, valor, unidade, fonte e ano. Eles só mudam por migration. |
| RN-03 | O fator de deslocamento é por (metodologia, modo, combustível), em kg CO₂e/t·km. O fator de etapa fixa é por (metodologia, fonte de energia), em kg CO₂e por unidade da fonte. |
| RN-04 | Uma combinação sem fator publicado não tem linha na tabela. Calcular com ela devolve 400 com a mensagem `"A metodologia DEFRA não tem fator para Aéreo + Diesel."`. |
| RN-05 | `GET /emission-factors` lista os fatores, para o front oferecer só as combinações que existem. A rota exige login, sem restrição de perfil. |

### Cálculo

| ID | Regra |
|---|---|
| RN-06 | Etapa de deslocamento: `co2Kg = distância (km) × peso da carga (t) × fator`. A distância, o modo e o combustível vêm do transporte da etapa. Sem transporte, a resposta é 400. |
| RN-07 | Peso da carga: se o produto do lote está em `KG` ou `TON`, o peso é a quantidade do lote convertida e o `cargoWeightKg` do pedido é ignorado. Em `LITER`, `UNIT` ou `M3`, o `cargoWeightKg` é obrigatório e maior que zero (400). |
| RN-08 | Etapa fixa: `co2Kg = consumo × fator`. O pedido traz a fonte (`ELECTRICITY` em kWh, `DIESEL` em litros, `LPG` em kg, `NATURAL_GAS` em m³) e o consumo, que é obrigatório e pode ser 0 (etapa que não gasta energia). Uma fonte por etapa. |
| RN-09 | A emissão grava o fator, a metodologia e o dado da atividade (peso da carga, ou fonte e consumo). Mudar a tabela de fatores depois não altera emissões já calculadas. |
| RN-10 | O transporte só pode ser registrado em etapa de deslocamento (400 nas fixas). |

### Obrigatoriedade

| ID | Regra |
|---|---|
| RN-11 | Uma etapa só é registrada se a etapa anterior do lote tem emissão. Senão, a resposta é 409 com a mensagem `"Calcule a emissão da etapa STORAGE antes de registrar a próxima."`. |
| RN-12 | A emissão é calculada uma vez por etapa. Uma segunda chamada devolve 409 com a mensagem `"A emissão da etapa 42 já foi calculada."` (D-7). Não há recálculo. |
| RN-13 | A emissão pode ser calculada a qualquer momento depois do registro da etapa, inclusive em lote `COMPLETED` (o Varejo é a última etapa). Quem pode calcular não muda (RN-10 da 017). |

### Score do ranking

| ID | Regra |
|---|---|
| RN-14 | `score = max(0, 100 + 10 × certificações ativas − intensidade / K)`, com 2 casas. `K` é uma constante da view, calibrada na Task 5 do plano. |
| RN-15 | Intensidade do fornecedor = soma do CO₂e dos lotes com peso conhecido ÷ soma dos pesos desses lotes em toneladas. O peso do lote é o `cargo_weight_kg` da emissão da etapa `TRANSPORT`. Lotes sem esse peso ficam fora da conta, tanto do numerador quanto do denominador. Sem nenhum lote com peso, a intensidade é 0. |
| RN-16 | `totalCo2Kg` continua no ranking como informação, mas não entra mais no score. |

### Dados legados

| ID | Regra |
|---|---|
| RN-17 | As emissões existentes com `IPCC` ou `EMEP_EEA` passam a `GHG_PROTOCOL`, com o mesmo fator e o mesmo CO₂. A troca fica na auditoria pelas triggers de `carbon_emission`. |
| RN-18 | Nas emissões existentes de etapa `TRANSPORT` cujo produto está em `KG` ou `TON`, a migration preenche o `cargo_weight_kg` pela quantidade do lote (RN-15). Nas demais emissões, o dado da atividade fica nulo. |
| RN-19 | Etapas antigas sem emissão seguem a RN-11: um lote em andamento cuja última etapa não tem emissão precisa dela antes da próxima etapa. O front oferece a ação de calcular a emissão pendente. |

## 5. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `POST /stages/{id}/emission` devolve 409 numa segunda chamada (RN-12). `GetBatchCarbonFootprintUseCase` usa o `CarbonFootprintCalculator` (D-11). |
| RF-02 | A migration `V22` cria `emission_factor` com os fatores do plano §2, adiciona `cargo_weight_kg`, `energy_source` e `consumption` em `carbon_emission`, remapeia o legado e reduz o enum (RN-01, RN-02, RN-17 e RN-18). |
| RF-03 | `GET /emission-factors` (RN-05). O front já sabe a unidade do lote pelo catálogo de produtos (`unitOf`), então `BatchResponseDTO` não muda. |
| RF-04 | `POST /stages/{id}/emission` calcula pela RN-06 a RN-09 e devolve o dado da atividade. `POST /transport` valida a RN-10. |
| RF-05 | `POST /batches/{id}/stages` valida a RN-11. |
| RF-06 | A migration `V23` recria `vw_supplier_sustainability` pela RN-14 a RN-16. |
| RNF-01 | Erros no formato atual: 400 (dado da atividade ausente ou inválido, combinação sem fator, transporte em etapa fixa), 403 (lote de outra companhia), 404 (etapa inexistente) e 409 (emissão repetida, etapa anterior sem emissão). |
| RNF-02 | Nenhuma dependência nova. Os fatores ficam no banco, não em código. |

## 6. Contratos

### `GET /api/v1/emission-factors`

```json
[
  { "calculationMethod": "DEFRA", "transportMode": "ROAD", "fuelType": "DIESEL", "energySource": null,
    "value": 0.10356, "unit": "kgCO2e/tkm", "source": "DESNZ 2026, Freighting goods, HGV all diesel, average laden", "referenceYear": 2026 },
  { "calculationMethod": "GHG_PROTOCOL", "transportMode": null, "fuelType": null, "energySource": "ELECTRICITY",
    "value": 0.0461, "unit": "kgCO2/kWh", "source": "MCTI/SIRENE, fator médio anual do SIN", "referenceYear": 2025 }
]
```

A tabela completa, com as fontes, fica no plano §2.

### `POST /api/v1/stages/{chainId}/emission`

Etapa de deslocamento (o `cargoWeightKg` só é lido quando o produto não está em `KG` ou `TON`):

```json
{ "calculationMethod": "DEFRA", "cargoWeightKg": 1200 }
```

Etapa fixa:

```json
{ "calculationMethod": "GHG_PROTOCOL", "energySource": "ELECTRICITY", "consumption": 350.5 }
```

A resposta `201` acrescenta os campos `cargoWeightKg`, `energySource` e `consumption` (nulos quando não se aplicam) aos campos atuais de `CarbonEmissionResponseDTO`. O `chainId` do corpo, que já era ignorado, deixa de existir.

### `POST /api/v1/batches/{batchId}/stages`

O contrato não muda. Etapa anterior sem emissão: `409`.

## 7. Fora do escopo

- Recalcular ou apagar uma emissão (RN-12).
- Mais de uma fonte de energia por etapa.
- CRUD de fatores: eles mudam por migration (RN-02).
- Fatores regionais ou por fornecedor.
- Peso por unidade no cadastro de produto.
- Mudar a soma dos relatórios e do dashboard por metodologia (D-9): a quebra `sumByMethod` do detalhe do relatório já mostra a mistura.
- Renomear `co2_kg` para `co2e_kg`.

## 8. Critérios de aceite

- Calcular a emissão de uma `PRODUCTION` com `ELECTRICITY` e 100 kWh pelo `GHG_PROTOCOL` grava `100 × fator do SIN` e devolve `energySource` e `consumption`.
- Calcular a emissão de um `TRANSPORT` rodoviário a diesel de 500 km, num lote de 2 TON, pelo `DEFRA`, grava `500 × 2 × fator`.
- O mesmo transporte num lote em `UNIT` sem `cargoWeightKg` devolve 400. Com `cargoWeightKg: 800`, usa 0,8 t.
- `AIR` + `DIESEL` devolve 400 com a mensagem da RN-04.
- Uma segunda chamada na mesma etapa devolve 409.
- Registrar a `STORAGE` com a `PRODUCTION` sem emissão devolve 409. Depois de calcular, devolve 201.
- `POST /transport` numa `STORAGE` devolve 400.
- Depois da `V22`, não há emissão com `IPCC` nem `EMEP_EEA`, e a soma de `co2_kg` é a mesma de antes.
- O ranking ordena pela intensidade: entre dois fornecedores sem certificação, o que tem menos kg CO₂e por tonelada fica à frente, mesmo com mais CO₂ total.
- O `./mvnw verify` passa.
