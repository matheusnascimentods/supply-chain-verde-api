# Plano técnico — Série mensal de emissões

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Consulta

```sql
SELECT month, SUM(co2_kg) AS co2_kg
FROM vw_monthly_emission
WHERE month BETWEEN :from AND :to
  AND (:supplierId IS NULL OR supplier_id = :supplierId)
GROUP BY month
ORDER BY month;
```

Os meses vazios são preenchidos em Java: percorre `YearMonth` de `from` até `to` e usa `0` quando o mês não veio na consulta. Mais simples e portável que `generate_series` (não existe no MySQL).

## 2. Camadas

| Item | Mudança |
|---|---|
| `DashboardController` | `GET /emissions` com `@RequestParam(required = false) YearMonth from, to` e `@AuthenticationPrincipal`. |
| `GetMonthlyEmissionsUseCase` (novo) | Aplica o padrão de 12 meses, valida o intervalo, define o `supplierId` pelo role e preenche os meses. |
| `CarbonEmissionRepository` | Consulta nativa na view. |
| `MonthlyEmissionDTO` (novo) | `record MonthlyEmissionDTO(String month, BigDecimal co2Kg)`. |
| `SecurityConfig` | Nada: cai no `anyRequest().authenticated()`. |

## 3. Testes

- Meses sem emissão aparecem com zero.
- `SUPPLIER` não recebe emissão de outro fornecedor.
- 400 para intervalo invertido e maior que 24 meses.
- Sem parâmetros: 12 itens terminando no mês atual.
