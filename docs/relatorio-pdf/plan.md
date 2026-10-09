# Plano técnico — Detalhe do relatório para o PDF

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Consultas

Três agregações sobre `vw_stage_emission`, todas com `supplier_id = :supplierId AND calculated_at BETWEEN :start AND :end`:

```sql
SELECT stage_type, SUM(co2_kg) FROM vw_stage_emission WHERE ... GROUP BY stage_type ORDER BY 2 DESC;

SELECT batch_id, product_name, quantity, unit, SUM(co2_kg)
FROM vw_stage_emission WHERE ... GROUP BY batch_id, product_name, quantity, unit ORDER BY batch_id;

SELECT calculation_method, emission_factor, SUM(co2_kg)
FROM vw_stage_emission WHERE ... GROUP BY calculation_method, emission_factor;
```

Certificações pela regra RN-02 no `CertificationRepository`. Endereço e telefone já vêm do `Supplier`.

## 2. Camadas

| Item | Mudança |
|---|---|
| `ReportDetailDTO` | Novos records aninhados: `ReportSupplierDTO`, `ReportEmissionsDTO` (`byStage`, `byBatch`, `byMethod`) e lista de `SupplierCertificationDTO` (já existe). |
| `GetReportUseCase` | Monta as novas seções depois de carregar o relatório. |
| `CarbonEmissionRepository` | As três consultas nativas. |
| `CertificationRepository` | `findBySupplierIdOverlapping(supplierId, start, end)`. |

## 3. Testes

- Emissão fora do período não entra.
- Certificação vencida antes do período não entra; vencida durante o período entra.
- `SUPPLIER` não lê relatório de outro fornecedor (403), como hoje.
