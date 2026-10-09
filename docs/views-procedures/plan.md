# Plano técnico — Views e procedures

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Migrations

| Migration | Conteúdo |
|---|---|
| `V32__create_emission_views.sql` | `vw_stage_emission`, `vw_supplier_product_emission`, `vw_monthly_emission` |
| `V33__create_report_and_certification_procedures.sql` | `sp_generate_sustainability_report`, `sp_expire_certifications` |

(Numeração supondo que [`V31`](../vinculo-usuario-fornecedor/plan.md) já exista.)

## 2. Views

```sql
CREATE OR REPLACE VIEW vw_stage_emission AS
SELECT b.batch_id,
       b.supplier_id,
       p.product_id,
       p.name        AS product_name,
       p.category,
       p.unit,
       b.quantity,
       c.chain_id,
       c.stage_type,
       ce.co2_kg,
       ce.emission_factor,
       ce.calculation_method,
       ce.calculated_at
FROM batch b
JOIN product p          ON p.product_id = b.product_id
JOIN chain c            ON c.batch_id = b.batch_id
JOIN carbon_emission ce ON ce.chain_id = c.chain_id;

-- Agrega primeiro por lote para não somar a quantidade uma vez por etapa.
CREATE OR REPLACE VIEW vw_supplier_product_emission AS
SELECT supplier_id,
       product_id,
       category,
       unit,
       COUNT(*)                          AS batch_count,
       SUM(batch_co2_kg)                 AS total_co2_kg,
       SUM(quantity)                     AS total_quantity,
       SUM(batch_co2_kg) / SUM(quantity) AS co2_kg_per_unit
FROM (
    SELECT batch_id, supplier_id, product_id, category, unit, quantity, SUM(co2_kg) AS batch_co2_kg
    FROM vw_stage_emission
    GROUP BY batch_id, supplier_id, product_id, category, unit, quantity
) per_batch
GROUP BY supplier_id, product_id, category, unit;

CREATE OR REPLACE VIEW vw_monthly_emission AS
SELECT supplier_id,
       CAST(date_trunc('month', calculated_at) AS DATE) AS month,
       SUM(co2_kg)                                      AS co2_kg
FROM vw_stage_emission
GROUP BY supplier_id, CAST(date_trunc('month', calculated_at) AS DATE);
```

MySQL: `date_trunc('month', x)` vira `DATE_FORMAT(x, '%Y-%m-01')`; o resto é igual.

## 3. Procedures

```sql
CREATE OR REPLACE PROCEDURE sp_generate_sustainability_report(
    p_supplier_id BIGINT,
    p_start DATE,
    p_end DATE,
    INOUT p_report_id BIGINT DEFAULT NULL
)
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO report (supplier_id, period_start_at, period_end_at, total_co2_kg, tracked_product_count)
    SELECT p_supplier_id, p_start, p_end,
           COALESCE(SUM(co2_kg), 0),
           COUNT(DISTINCT product_id)
    FROM vw_stage_emission
    WHERE supplier_id = p_supplier_id
      AND calculated_at BETWEEN p_start AND p_end
    RETURNING report_id INTO p_report_id;
END;
$$;

CREATE OR REPLACE PROCEDURE sp_expire_certifications(INOUT p_expired_count INTEGER DEFAULT 0)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE certification
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE' AND expires_at < CURRENT_DATE;
    GET DIAGNOSTICS p_expired_count = ROW_COUNT;
END;
$$;
```

MySQL: `CREATE PROCEDURE ... (IN ..., OUT ...) BEGIN ... END` com `DELIMITER`; `RETURNING` vira `LAST_INSERT_ID()` e `GET DIAGNOSTICS` vira `ROW_COUNT()`.

As validações de entrada (fornecedor existe, `p_end >= p_start`) continuam no caso de uso, que já devolve as mensagens de erro da API. A `CHECK` de `report` cobre o período no banco.

## 4. Consumo na API

| Ponto | Como |
|---|---|
| Views | Consultas nativas no repositório (`@Query(nativeQuery = true)`) ou entidade `@Immutable` mapeada na view. Views não recebem escrita. |
| `sp_generate_sustainability_report` | `CALL sp_generate_sustainability_report(:supplierId, :start, :end, NULL)` via `EntityManager.createStoredProcedureQuery` ou `JdbcTemplate`, dentro do `@Transactional` do caso de uso (o contexto de auditoria `app.user_id` precisa estar na mesma conexão). |
| `sp_expire_certifications` | `@EnableScheduling` + `@Scheduled(cron = "0 0 3 * * *")` num componente de infraestrutura. |

| Caso de uso | Passa a ler |
|---|---|
| `GenerateSustainabilityReportUseCase` | procedure |
| `GetDashboardSummaryUseCase` (`monthlyEmissionKgCo2e`) | `vw_monthly_emission` |
| `RankSuppliersBySustainabilityUseCase` (recomendação) | `vw_supplier_product_emission` |
| `GetReportUseCase` (detalhe do PDF) | `vw_stage_emission` |

## 5. Consultas para a apresentação

`docs/sql/consultas-relatorio.sql`, só leitura, por exemplo:

```sql
-- Top 5 fornecedores com menor CO₂ por kg de produtos agrícolas
SELECT s.name, v.co2_kg_per_unit
FROM vw_supplier_product_emission v
JOIN supplier s ON s.supplier_id = v.supplier_id
WHERE v.category = 'AGRICULTURE' AND v.unit = 'KG'
ORDER BY v.co2_kg_per_unit
LIMIT 5;

-- Evolução mensal da cadeia inteira
SELECT month, SUM(co2_kg) AS co2_kg FROM vw_monthly_emission GROUP BY month ORDER BY month;

-- Etapa que mais emite
SELECT stage_type, SUM(co2_kg) FROM vw_stage_emission GROUP BY stage_type ORDER BY 2 DESC;
```

## 6. Testes

- Testcontainers: cada view contra um cenário montado (2 fornecedores, lotes com e sem emissão, unidades diferentes), conferindo `co2_kg_per_unit` à mão.
- Procedure de relatório: totais no período, fora do período e fornecedor sem emissão (total 0).
- Procedure de expiração: só `ACTIVE` vencidas mudam; `SUSPENDED` vencida não muda; o `audit_log` recebe as linhas.
