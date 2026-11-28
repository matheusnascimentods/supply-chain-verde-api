-- Current definition from V3__create_views.sql.
CREATE VIEW vw_batch_current_stage AS
SELECT batch_id, chain_id, stage_type, started_at
FROM (SELECT batch_id,
             id AS chain_id,
             stage_type,
             started_at,
             ROW_NUMBER() OVER (PARTITION BY batch_id ORDER BY started_at DESC, id DESC) AS position
      FROM chain) ranked
WHERE position = 1;
