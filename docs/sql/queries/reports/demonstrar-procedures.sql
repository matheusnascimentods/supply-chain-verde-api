-- Procedures alteram dados; use a transação para demonstrar e depois desfazer.
START TRANSACTION;
CALL sp_generate_sustainability_report(1, DATE '2025-01-01', DATE '2026-12-31', @report_id);
SELECT @report_id;
CALL sp_expire_certifications(@expired_count);
SELECT @expired_count;
ROLLBACK;
