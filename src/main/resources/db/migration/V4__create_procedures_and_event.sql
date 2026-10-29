-- SQL SECURITY INVOKER: rodam com os privilégios de quem chama, não do root que criou.
DELIMITER $$

CREATE PROCEDURE sp_generate_sustainability_report(
    IN p_supplier_id BIGINT, IN p_start DATE, IN p_end DATE, OUT p_report_id BIGINT)
SQL SECURITY INVOKER
BEGIN
    INSERT INTO report (supplier_id, period_start_at, period_end_at, total_co2_kg, batch_count, tracked_product_count)
    SELECT p_supplier_id, p_start, p_end,
           COALESCE(SUM(co2_kg), 0),
           COUNT(DISTINCT batch_id),
           COUNT(DISTINCT product_id)
    FROM vw_stage_emission
    WHERE supplier_id = p_supplier_id
      AND calculated_at BETWEEN p_start AND p_end;
    SET p_report_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_expire_certifications(OUT p_expired_count INT)
SQL SECURITY INVOKER
BEGIN
    UPDATE certification
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE' AND expires_at < CURRENT_DATE;
    SET p_expired_count = ROW_COUNT();
END$$

DELIMITER ;

-- Meia-noite de São Paulo, o fuso do servidor. O event_scheduler vem ligado no MySQL 8.4.
CREATE EVENT ev_expire_certifications
ON SCHEDULE EVERY 1 DAY STARTS (CURRENT_DATE + INTERVAL 1 DAY)
DO CALL sp_expire_certifications(@expired_count);
