-- Current definition from V4__create_procedures_and_event.sql.
DELIMITER $$
CREATE PROCEDURE sp_expire_certifications(OUT p_expired_count INT)
SQL SECURITY INVOKER
BEGIN
    UPDATE certification
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE' AND expires_at < CURRENT_DATE;
    SET p_expired_count = ROW_COUNT();
END$$
DELIMITER ;
