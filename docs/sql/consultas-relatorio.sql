-- Consultas de relatório sobre as views e procedures (A3, entregável #4).
-- Só leitura, exceto os CALL do fim. Rodar com: mysql -u root -p supply_chain_verde < docs/sql/consultas-relatorio.sql

-- Top 5 fornecedores com menor CO₂ por kg de produtos agrícolas
SELECT s.name, v.co2_kg_per_unit
FROM vw_supplier_product_emission v
JOIN supplier s ON s.id = v.supplier_id
WHERE v.category = 'AGRICULTURE' AND v.unit = 'KG'
ORDER BY v.co2_kg_per_unit
LIMIT 5;

-- Evolução mensal da cadeia inteira
SELECT month, SUM(co2_kg) AS co2_kg
FROM vw_monthly_emission
GROUP BY month
ORDER BY month;

-- Etapa que mais emite
SELECT stage_type, SUM(co2_kg) AS co2_kg
FROM vw_stage_emission
GROUP BY stage_type
ORDER BY 2 DESC;

-- Ranking de sustentabilidade (mesma ordem de GET /suppliers?ranked=true)
SELECT s.name, v.sustainability_score, v.active_certifications, v.total_co2_kg
FROM vw_supplier_sustainability v
JOIN supplier s ON s.id = v.supplier_id
ORDER BY v.sustainability_score DESC, v.supplier_id
LIMIT 10;

-- Lotes por etapa atual (cards do dashboard)
SELECT stage_type, COUNT(*) AS lotes
FROM vw_batch_current_stage
GROUP BY stage_type
ORDER BY 2 DESC;

-- Relatórios: retrato gravado na geração (lotes ≥ produtos)
SELECT r.id, s.name, r.period_start_at, r.period_end_at, r.total_co2_kg, r.batch_count, r.tracked_product_count
FROM report r
JOIN supplier s ON s.id = r.supplier_id
ORDER BY r.created_at DESC
LIMIT 10;

-- Agendamento do event scheduler
SELECT event_name, interval_value, interval_field, starts, last_executed, status
FROM information_schema.events
WHERE event_schema = 'supply_chain_verde';

-- Procedures (escrevem dados; rodar dentro de uma transação para demonstrar e desfazer)
START TRANSACTION;
CALL sp_generate_sustainability_report(1, DATE '2025-01-01', DATE '2026-12-31', @report_id);
SELECT @report_id;
CALL sp_expire_certifications(@expired_count);
SELECT @expired_count;
ROLLBACK;
