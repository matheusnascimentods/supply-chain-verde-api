-- Sequência estrita: PRODUCTION → STORAGE → PROCESSING → TRANSPORT → DISTRIBUTION → RETAIL.

-- 1. Lotes existentes: insere as etapas que faltam antes da etapa mais avançada de cada lote,
--    1 minuto antes da próxima etapa existente e com o mesmo responsável.
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

-- 2. A etapa anterior é função do tipo; PRODUCTION fica NULL e por isso dispensa a FK.
ALTER TABLE chain ADD COLUMN previous_stage_type stage_type GENERATED ALWAYS AS (
    CASE stage_type
        WHEN 'STORAGE'      THEN 'PRODUCTION'::stage_type
        WHEN 'PROCESSING'   THEN 'STORAGE'::stage_type
        WHEN 'TRANSPORT'    THEN 'PROCESSING'::stage_type
        WHEN 'DISTRIBUTION' THEN 'TRANSPORT'::stage_type
        WHEN 'RETAIL'       THEN 'DISTRIBUTION'::stage_type
    END) STORED;

-- 3. Um lote não repete etapa, e cada etapa exige a anterior no mesmo lote (e impede apagá-la).
ALTER TABLE chain ADD CONSTRAINT uq_chain_batch_stage UNIQUE (batch_id, stage_type);

ALTER TABLE chain ADD CONSTRAINT fk_chain_previous_stage
    FOREIGN KEY (batch_id, previous_stage_type) REFERENCES chain (batch_id, stage_type);
