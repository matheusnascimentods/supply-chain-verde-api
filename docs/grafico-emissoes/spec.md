# Especificação — Série mensal de emissões

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/grafico-emissoes/`.

## 1. Problema

A A3 pede impacto mensurável. O dashboard mostra só a emissão do mês corrente, então não dá para ver se as emissões estão caindo.

## 2. Objetivo

Endpoint com a emissão de CO₂ mês a mês num intervalo escolhido, para o gráfico de linha do dashboard.

## 3. Regras

| ID | Regra |
|---|---|
| RN-01 | Escopo por role: `ADMIN`, `MANAGER` e `AUDITOR` recebem a cadeia inteira; `SUPPLIER` recebe só as emissões dos próprios lotes (via `supplierId` do token, ver [vínculo](../vinculo-usuario-fornecedor/spec.md)). |
| RN-02 | O mês de uma emissão é o mês de `carbon_emission.calculated_at` (mesma regra do card "Emissão Total (mês)"). |
| RN-03 | Todos os meses do intervalo aparecem, inclusive os sem emissão (`co2Kg = 0`), para a linha não pular meses. |
| RN-04 | Intervalo máximo de 24 meses. Sem parâmetros: os últimos 12 meses, incluindo o atual. |

## 4. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `GET /dashboard/emissions?from=YYYY-MM&to=YYYY-MM`, para qualquer usuário autenticado. |
| RF-02 | `from > to` ou intervalo maior que 24 meses retorna 400. |
| RNF-01 | Leitura de `vw_monthly_emission` ([views-procedures](../views-procedures/spec.md)). |

## 5. Contrato

```http
GET /api/v1/dashboard/emissions?from=2026-01&to=2026-06
```

```json
[
  { "month": "2026-01", "co2Kg": 1250.40 },
  { "month": "2026-02", "co2Kg": 1102.75 },
  { "month": "2026-03", "co2Kg": 0 },
  { "month": "2026-04", "co2Kg": 980.10 },
  { "month": "2026-05", "co2Kg": 945.00 },
  { "month": "2026-06", "co2Kg": 870.33 }
]
```

## 6. Fora de escopo

- Quebra por etapa da cadeia ou por fornecedor no mesmo gráfico.
- Escopo por role nos cards do resumo (`/dashboard/summary` continua global).
