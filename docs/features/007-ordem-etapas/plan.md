# Plano técnico — Ordem obrigatória das etapas

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Por que constraint e não trigger

Um `CHECK` só enxerga a própria linha, então não sabe quais etapas o lote já tem. Com sequência estrita, porém, a etapa anterior é função do próprio `stage_type`. Ela vira uma coluna gerada, e uma FK composta da tabela para ela mesma exige que essa etapa exista no lote:

| Constraint | Garante |
|---|---|
| `previous_stage_type` gerada + `fk_chain_previous_stage` | RN-02: a etapa anterior existe no mesmo lote. `PRODUCTION` tem anterior `NULL`, e FK com `NULL` não é verificada (`MATCH SIMPLE`), então só ela entra sem dependência. |
| `uq_chain_batch_stage` | RN-03, e é o alvo que a FK exige. |
| `fk_chain_previous_stage` (`ON DELETE` padrão) | RN-04: apagar uma etapa referenciada pela seguinte falha. |

Testado no PostgreSQL 17 local: lote sem a etapa anterior, etapa repetida e `DELETE` de etapa do meio são recusados.

## 2. Migration `V39__enforce_stage_order.sql`

```sql
-- RN-05: insere as etapas que faltam antes da etapa mais avançada de cada lote.
-- Ficam 1 minuto antes da próxima etapa existente, com o mesmo responsável.
WITH stage_order AS (
    SELECT s AS stage_type, array_position(enum_range(NULL::stage_type), s) AS pos
    FROM unnest(enum_range(NULL::stage_type)) s
), existing AS (
    SELECT c.*, o.pos FROM chain c JOIN stage_order o USING (stage_type)
), missing AS (
    SELECT b.batch_id, o.stage_type, o.pos
    FROM (SELECT batch_id, MAX(pos) AS max_pos FROM existing GROUP BY batch_id) b
    JOIN stage_order o ON o.pos < b.max_pos
    WHERE NOT EXISTS (SELECT 1 FROM existing e WHERE e.batch_id = b.batch_id AND e.stage_type = o.stage_type)
)
INSERT INTO chain (batch_id, responsible_user_id, stage_type, started_at, ended_at)
SELECT m.batch_id, nxt.responsible_user_id, m.stage_type,
       nxt.started_at - make_interval(mins => nxt.pos - m.pos),
       nxt.started_at - make_interval(mins => nxt.pos - m.pos)
FROM missing m
CROSS JOIN LATERAL (
    SELECT e.responsible_user_id, e.started_at, e.pos FROM existing e
    WHERE e.batch_id = m.batch_id AND e.pos > m.pos ORDER BY e.pos LIMIT 1
) nxt;

ALTER TABLE chain ADD COLUMN previous_stage_type stage_type GENERATED ALWAYS AS (
    CASE stage_type
        WHEN 'STORAGE'      THEN 'PRODUCTION'::stage_type
        WHEN 'PROCESSING'   THEN 'STORAGE'::stage_type
        WHEN 'TRANSPORT'    THEN 'PROCESSING'::stage_type
        WHEN 'DISTRIBUTION' THEN 'TRANSPORT'::stage_type
        WHEN 'RETAIL'       THEN 'DISTRIBUTION'::stage_type
    END) STORED;

ALTER TABLE chain ADD CONSTRAINT uq_chain_batch_stage UNIQUE (batch_id, stage_type);

ALTER TABLE chain ADD CONSTRAINT fk_chain_previous_stage
    FOREIGN KEY (batch_id, previous_stage_type) REFERENCES chain (batch_id, stage_type);
```

Ensaiada no banco local (transação com `ROLLBACK`): 82 etapas inseridas, as 3 constraints criadas e 82 linhas novas no `audit_log`. Sequências resultantes:

| Sequência | Lotes |
|---|---|
| `PRODUCTION>STORAGE>PROCESSING` | 20 |
| `PRODUCTION>STORAGE>PROCESSING>TRANSPORT` | 16 |
| `PRODUCTION` | 16 |
| `PRODUCTION>STORAGE>PROCESSING>TRANSPORT>DISTRIBUTION>RETAIL` | 13 |
| `PRODUCTION>STORAGE>PROCESSING>TRANSPORT>DISTRIBUTION` | 4 |

As migrations de seed já aplicadas (`V18`, `V28`) não são editadas: o checksum do Flyway quebraria.

## 3. API

| Ponto | Mudança |
|---|---|
| `StageType` | `next()`: a etapa seguinte pela ordem do enum, `null` depois de `RETAIL`. |
| `RegisterChainStageUseCase` | Lê `chainRepository.findByBatchId(batchId)`. Esperado = `PRODUCTION` se o lote não tem etapas, senão `next()` da etapa mais avançada. Se `request.stageType()` for diferente, lança `InvalidStageTransitionException(atual, pedida)`. |
| `GlobalExceptionHandler` | `DataIntegrityViolationException` em `uq_chain_batch_stage` ou `fk_chain_previous_stage` → mesmo status de `InvalidStageTransitionException`. Outras violações seguem como hoje. |
| `ChainJpaEntity` | Não mapeia `previous_stage_type` (`ddl-auto=validate` aceita coluna não mapeada). |

## 4. Testes

- Use case: lote vazio aceita só `PRODUCTION`; pular etapa, repetir etapa e etapa depois de `RETAIL` lançam `InvalidStageTransitionException`.
- Integração (Testcontainers): `INSERT` direto fora de ordem falha na FK; `DELETE` de etapa do meio falha; a API responde ao conflito sem 500.
- Os fixtures atuais (`SupplierRecommendationIT`) só inserem `PRODUCTION` e continuam válidos.
