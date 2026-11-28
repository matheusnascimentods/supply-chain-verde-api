-- Current definition from V5__create_audit.sql.
DELIMITER $$
CREATE DEFINER = 'supply_audit'@'%' PROCEDURE audit_write(
    p_table VARCHAR(120), p_id BIGINT, p_op VARCHAR(6), p_before JSON, p_after JSON, p_status_changed BOOLEAN)
SQL SECURITY DEFINER
body: BEGIN
    IF @app_user_id IS NOT NULL AND @app_user_id NOT REGEXP '^[0-9]+$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid app_user_id audit context';
    END IF;
    -- UPDATE sem mudança não gera evento (RN-02).
    IF p_op = 'UPDATE' AND JSON_LENGTH(p_after) = 0 THEN
        LEAVE body;
    END IF;
    INSERT INTO audit_log (user_id, action, affected_table, affected_entity_id, before_data, after_data, created_at)
    VALUES (CAST(@app_user_id AS UNSIGNED),
            IF(p_op = 'UPDATE' AND p_status_changed, 'STATUS_CHANGE', p_op),
            p_table, p_id, p_before, p_after, NOW(6));
END$$
DELIMITER ;
