-- Lotes agrupados pela etapa atual, como nos cards do dashboard.
SELECT stage_type, COUNT(*) AS lotes
FROM vw_batch_current_stage
GROUP BY stage_type
ORDER BY 2 DESC;
