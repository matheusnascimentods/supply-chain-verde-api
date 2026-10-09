# Especificação — Detalhe do relatório para o PDF

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). O PDF é montado no frontend: `supply-chain-verde-web/docs/relatorio-pdf/`.

## 1. Problema

O tema da A3 pede "relatórios para certificação". A API grava o relatório, mas o detalhe (`GET /reports?reportId=`) traz só totais; não há dados suficientes para um documento que um auditor ou certificadora leia.

## 2. Objetivo

Enriquecer o detalhe do relatório com tudo o que o PDF precisa. A API só entrega dados; o template e a geração do PDF ficam no frontend.

## 3. Regras

| ID | Regra |
|---|---|
| RN-01 | Os dados do detalhe se referem ao período do relatório (`period_start_at` a `period_end_at`), pela data do cálculo da emissão ([views-procedures](../views-procedures/spec.md), RN-01). |
| RN-02 | Certificações do período: as que estiveram vigentes em algum momento do período (`issued_at <= period_end_at` e `expires_at >= period_start_at`), com o status atual. |
| RN-03 | Acesso igual ao de hoje: `ADMIN`, `MANAGER`, `AUDITOR` e o `SUPPLIER` dono do relatório. |

## 4. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `ReportDetailDTO` ganha as seções `supplier`, `emissions` e `certifications` descritas abaixo. Os campos atuais continuam iguais (compatível). |
| RF-02 | Emissões por etapa, por lote e por metodologia (com o fator usado). |
| RNF-01 | Leitura de `vw_stage_emission`. |

## 5. Contrato

`GET /api/v1/reports?reportId=15`

```json
{
  "reportId": 15,
  "supplierId": 3,
  "supplierCnpj": "12345678000190",
  "supplierName": "Fazenda Verde",
  "periodStartAt": "2026-01-01",
  "periodEndAt": "2026-06-30",
  "totalCo2Kg": 6148.58,
  "totalBatchCount": 9,
  "trackedProductCount": 3,
  "generatedAt": "2026-07-01T09:30:00",

  "supplier": {
    "phone": "11999990000",
    "address": { "street": "...", "number": "10", "neighborhood": "...", "complement": null, "zipCode": "01001000", "city": "São Paulo", "state": "SP" }
  },
  "emissions": {
    "byStage": [ { "stageType": "TRANSPORT", "co2Kg": 3100.20 } ],
    "byBatch": [ { "batchId": 42, "productName": "Café orgânico", "quantity": 500, "unit": "KG", "co2Kg": 812.40 } ],
    "byMethod": [ { "calculationMethod": "GHG_PROTOCOL", "emissionFactor": 0.21500000, "co2Kg": 4020.00 } ]
  },
  "certifications": [
    { "certification": "Orgânico Brasil", "issuingBody": "IBD", "issuedAt": "2025-03-01", "expiresAt": "2027-03-01", "status": "ACTIVE" }
  ]
}
```

## 6. Fora de escopo

- Geração de PDF no backend.
- Comparativo com o período anterior e posição no ranking.
