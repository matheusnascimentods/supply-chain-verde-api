-- Current definition from V9__drop_supplier.sql.
DELIMITER $$
CREATE PROCEDURE sp_generate_sustainability_report(
    IN p_supplier_id BIGINT, IN p_market_id BIGINT, IN p_start DATE, IN p_end DATE, OUT p_report_id BIGINT)
SQL SECURITY INVOKER
BEGIN
    INSERT INTO report (supplier_id, market_id, period_start_at, period_end_at, total_co2_kg, batch_count,
                        tracked_product_count)
    SELECT p_supplier_id, p_market_id, p_start, p_end,
           COALESCE(SUM(co2_kg), 0),
           COUNT(DISTINCT batch_id),
           COUNT(DISTINCT product_id)
    FROM vw_stage_emission
    WHERE supplier_id = p_supplier_id
      AND market_id = p_market_id
      AND calculated_at BETWEEN p_start AND p_end;
    SET p_report_id = LAST_INSERT_ID();
END$$
DELIMITER ;
