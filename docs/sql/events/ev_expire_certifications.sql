-- Current definition from V4__create_procedures_and_event.sql.
CREATE EVENT ev_expire_certifications
ON SCHEDULE EVERY 1 DAY STARTS (CURRENT_DATE + INTERVAL 1 DAY)
DO CALL sp_expire_certifications(@expired_count);
