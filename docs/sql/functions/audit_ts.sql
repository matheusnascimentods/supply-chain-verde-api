-- Current definition from V5__create_audit.sql.
DELIMITER $$
CREATE FUNCTION audit_ts(p_value DATETIME(6)) RETURNS VARCHAR(40)
DETERMINISTIC NO SQL
RETURN DATE_FORMAT(p_value, '%Y-%m-%dT%H:%i:%s.%f-03:00')$$
DELIMITER ;
