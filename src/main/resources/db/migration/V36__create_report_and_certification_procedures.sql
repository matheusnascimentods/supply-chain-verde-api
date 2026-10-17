-- O relatório é um retrato: CO₂, lotes e produtos são gravados juntos na geração.
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

CREATE PROCEDURE sp_generate_sustainability_report(
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

CREATE PROCEDURE sp_expire_certifications(INOUT p_expired_count INTEGER DEFAULT 0)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE certification
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE' AND expires_at < CURRENT_DATE;
    GET DIAGNOSTICS p_expired_count = ROW_COUNT;
END;
$$;
