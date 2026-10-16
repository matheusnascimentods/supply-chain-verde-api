# Plano técnico — Views e procedures

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Migrations

| Migration | Conteúdo |
|---|---|
| `V31__create_emission_views.sql` | `vw_stage_emission`, `vw_supplier_product_emission` |
| `V33__create_monthly_emission_view.sql` | `vw_monthly_emission` (criada no [gráfico de emissões](../003-grafico-emissoes/plan.md)) |
| `V36__create_report_and_certification_procedures.sql` | `sp_generate_sustainability_report`, `sp_expire_certifications` |
| `V37__schedule_certification_expiration.sql` | `CREATE EXTENSION pg_cron` + agendamento de `sp_expire_certifications` |
| `V38__create_sustainability_and_current_stage_views.sql` | `vw_supplier_sustainability`, `vw_batch_current_stage` |

(A `V32` fica com o [vínculo usuário-fornecedor](../002-vinculo-usuario-fornecedor/plan.md); a `V34` e a `V35` com a [padronização do schema](../005-padronizacao-schema/plan.md), que recria `vw_stage_emission` com os nomes novos e mantém as colunas de saída abaixo.)

## 2. Views

```sql
CREATE OR REPLACE VIEW vw_stage_emission AS
SELECT b.id          AS batch_id,
       b.supplier_id,
       p.id          AS product_id,
       p.name        AS product_name,
       p.category,
       p.unit,
       b.quantity,
       c.id          AS chain_id,
       c.stage_type,
       ce.co2_kg,
       ce.emission_factor,
       ce.calculation_method,
       CAST(ce.created_at AT TIME ZONE 'America/Sao_Paulo' AS DATE) AS calculated_at
FROM batch b
JOIN product p          ON p.id = b.product_id
JOIN chain c            ON c.batch_id = b.id
JOIN carbon_emission ce ON ce.chain_id = c.id;

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

```sql
-- RN-05. Mesma fórmula do SustainabilityScoreCalculator, que é removido.
CREATE VIEW vw_supplier_sustainability AS
SELECT s.id                                 AS supplier_id,
       COALESCE(c.active_certifications, 0) AS active_certifications,
       COALESCE(e.total_co2_kg, 0)          AS total_co2_kg,
       GREATEST(0, 100 + 10 * COALESCE(c.active_certifications, 0)
                       - ROUND(COALESCE(e.total_co2_kg, 0) / 100, 2))::NUMERIC(10, 2) AS sustainability_score
FROM supplier s
LEFT JOIN (SELECT supplier_id, COUNT(*) AS active_certifications
           FROM certification WHERE status = 'ACTIVE' GROUP BY supplier_id) c ON c.supplier_id = s.id
LEFT JOIN (SELECT supplier_id, SUM(co2_kg) AS total_co2_kg
           FROM vw_stage_emission GROUP BY supplier_id) e ON e.supplier_id = s.id;

-- RN-06. Substitui o CROSS JOIN LATERAL repetido no BatchJpaRepository.
CREATE VIEW vw_batch_current_stage AS
SELECT DISTINCT ON (c.batch_id)
       c.batch_id, c.id AS chain_id, c.stage_type, c.started_at
FROM chain c
ORDER BY c.batch_id, c.started_at DESC, c.id DESC;
```

Validado no banco local: o score bate com o do Java (fornecedor 35: 3 certificações, 6,37 kg → 129,94) e `vw_batch_current_stage` dá os mesmos 56 lotes ativos da query atual.

## 3. Procedures

```sql
-- Mesma migration: o retrato guarda também o nº de lotes (RN-07).
ALTER TABLE report ADD COLUMN batch_count INTEGER NOT NULL DEFAULT 0
    CONSTRAINT chk_report_batch_count_non_negative CHECK (batch_count >= 0);

-- Relatórios já gravados: sem isso apareceriam com 0 lotes, e os gerados pela API seguiriam com CO₂ 0.
UPDATE report r
SET total_co2_kg = t.co2, batch_count = t.batches, tracked_product_count = t.products
FROM (SELECT r2.id, COALESCE(SUM(v.co2_kg), 0) AS co2,
             COUNT(DISTINCT v.batch_id) AS batches, COUNT(DISTINCT v.product_id) AS products
      FROM report r2
      LEFT JOIN vw_stage_emission v
             ON v.supplier_id = r2.supplier_id AND v.calculated_at BETWEEN r2.period_start_at AND r2.period_end_at
      GROUP BY r2.id) t
WHERE r.id = t.id;

CREATE OR REPLACE PROCEDURE sp_generate_sustainability_report(
    p_supplier_id BIGINT,
    p_start DATE,
    p_end DATE,
    INOUT p_report_id BIGINT DEFAULT NULL
)
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO report (supplier_id, period_start_at, period_end_at, total_co2_kg, batch_count, tracked_product_count)
    SELECT p_supplier_id, p_start, p_end,
           COALESCE(SUM(co2_kg), 0),
           COUNT(DISTINCT batch_id),
           COUNT(DISTINCT product_id)
    FROM vw_stage_emission
    WHERE supplier_id = p_supplier_id
      AND calculated_at BETWEEN p_start AND p_end
    RETURNING id INTO p_report_id;
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

As validações de entrada (fornecedor existe, `p_end >= p_start`) continuam no caso de uso, que já devolve as mensagens de erro da API. A `CHECK` de `report` cobre o período no banco.

## 4. Consumo na API

| Ponto | Como |
|---|---|
| Views | Consultas nativas no repositório (`@Query(nativeQuery = true)`) ou entidade `@Immutable` mapeada na view. Views não recebem escrita. |
| `sp_generate_sustainability_report` | `CALL sp_generate_sustainability_report(:supplierId, :start, :end, NULL)` via `EntityManager.createStoredProcedureQuery` ou `JdbcTemplate`, dentro do `@Transactional` do caso de uso (o contexto de auditoria `app.user_id` precisa estar na mesma conexão). |
| `sp_expire_certifications` | Não passa pela API: o `pg_cron` chama (§4.1). |

| Caso de uso | Passa a ler |
|---|---|
| `GenerateSustainabilityReportUseCase` | procedure |
| `GetDashboardSummaryUseCase` (`monthlyEmissionKgCo2e`) | `vw_monthly_emission` |
| `RankSuppliersBySustainabilityUseCase` (recomendação) | `vw_supplier_product_emission` |
| `RankSuppliersBySustainabilityUseCase` (ranking) | `vw_supplier_sustainability` com busca, filtro `user_id IS NULL`, `ORDER BY sustainability_score DESC, supplier_id` e `LIMIT/OFFSET` no SQL. Certificações da página em uma consulta só (`supplier_id IN (...)`), como já é feito com a contagem de relatórios. |
| `BatchJpaRepository.countActiveBatches` / `findRecentWithLatestStage` | `vw_batch_current_stage` |
| `GetReportUseCase` (detalhe do PDF) | `vw_stage_emission` para as tabelas; `report.batch_count` para o total de lotes |
| `ListReportsUseCase` | `report.batch_count`; sai o `countBySupplierIdAndProducedAtBetween` por relatório (N+1) |

### 4.1 `pg_cron`

```sql
-- V37
CREATE EXTENSION IF NOT EXISTS pg_cron;
SELECT cron.schedule('expire-certifications', '0 3 * * *', 'CALL sp_expire_certifications()');
```

O `pg_cron` não vem nas imagens oficiais do Postgres e precisa ser carregado na inicialização:

| Onde | Mudança |
|---|---|
| `docker/postgres/Dockerfile` | `FROM postgres:17` (Debian; a variante alpine não tem o pacote) + `apt-get install -y postgresql-17-cron`. |
| `docker-compose.yml` | `build: docker/postgres` no lugar de `image:` e `command: postgres -c shared_preload_libraries=pg_cron -c cron.database_name=supply_chain_verde`. |
| `TestcontainersConfiguration` | Mesma imagem (`ImageFromDockerfile` compatível com `postgres`) e o mesmo `command`, com `cron.database_name` igual ao banco do container. Sem isso a `V37` quebra o `./mvnw verify`. |

- `cron.database_name` precisa ser o banco da aplicação: a extensão só pode ser criada nele, e é nele que o job roda.
- O `pg_cron` usa UTC: `0 3 * * *` é meia-noite em São Paulo, e o `CURRENT_DATE` da procedure já é a data nova.
- `CREATE EXTENSION` exige superusuário. Localmente o usuário é `postgres`; na Oracle Cloud é preciso confirmar.
- Acompanhamento: `SELECT * FROM cron.job;` e `SELECT status, return_message, start_time FROM cron.job_run_details ORDER BY start_time DESC;`. Isso entra no plano de monitoramento da A3.

## 5. Consultas para a apresentação

`docs/sql/consultas-relatorio.sql`, só leitura, por exemplo:

```sql
-- Top 5 fornecedores com menor CO₂ por kg de produtos agrícolas
SELECT s.name, v.co2_kg_per_unit
FROM vw_supplier_product_emission v
JOIN supplier s ON s.id = v.supplier_id
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
- Procedure de relatório: totais no período, fora do período e fornecedor sem emissão (total 0). Dois lotes do mesmo produto → `batch_count = 2`, `tracked_product_count = 1`. Lote novo depois da geração não muda o relatório já gravado.
- Procedure de expiração: só `ACTIVE` vencidas mudam; `SUSPENDED` vencida não muda; o `audit_log` recebe as linhas.
- `vw_supplier_sustainability`: os casos do `SustainabilityScoreCalculatorTest` viram teste de integração da view (sem certificação nem emissão → 100; penalidade que levaria abaixo de 0 → 0; certificação `EXPIRED` não conta).
- `vw_batch_current_stage`: desempate por `id` quando duas etapas têm o mesmo `started_at`; lote sem etapa fora da view.
