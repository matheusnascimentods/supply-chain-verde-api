-- Feature 010: views, procedure, auditoria e grants passam a ler company; depois supplier sai.

-- Views: emissões expõem o mercado do lote; o score continua global, porque é do fornecedor (RN-13).
CREATE OR REPLACE VIEW vw_stage_emission AS
SELECT b.id AS batch_id,
       b.supplier_id,
       b.market_id,
       p.id AS product_id,
       p.name AS product_name,
       p.category,
       p.unit,
       b.quantity,
       c.id AS chain_id,
       c.stage_type,
       ce.co2_kg,
       ce.emission_factor,
       ce.calculation_method,
       DATE(ce.created_at) AS calculated_at
FROM batch b
JOIN product p ON p.id = b.product_id
JOIN chain c ON c.batch_id = b.id
JOIN carbon_emission ce ON ce.chain_id = c.id;

CREATE OR REPLACE VIEW vw_monthly_emission AS
SELECT supplier_id,
       market_id,
       CAST(DATE_FORMAT(calculated_at, '%Y-%m-01') AS DATE) AS month,
       SUM(co2_kg) AS co2_kg
FROM vw_stage_emission
GROUP BY supplier_id, market_id, month;

CREATE OR REPLACE VIEW vw_supplier_sustainability AS
SELECT s.id AS supplier_id,
       COALESCE(c.active_certifications, 0) AS active_certifications,
       COALESCE(e.total_co2_kg, 0) AS total_co2_kg,
       CAST(GREATEST(0, 100 + 10 * COALESCE(c.active_certifications, 0)
                        - ROUND(COALESCE(e.total_co2_kg, 0) / 100, 2)) AS DECIMAL(10,2)) AS sustainability_score
FROM company s
LEFT JOIN (SELECT supplier_id, COUNT(*) AS active_certifications
           FROM certification
           WHERE status = 'ACTIVE'
           GROUP BY supplier_id) c ON c.supplier_id = s.id
LEFT JOIN (SELECT supplier_id, SUM(co2_kg) AS total_co2_kg
           FROM vw_stage_emission
           GROUP BY supplier_id) e ON e.supplier_id = s.id
WHERE s.type = 'SUPPLIER';

-- RN-23: o relatório é do mercado e só conta os lotes dele.
DROP PROCEDURE sp_generate_sustainability_report;

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

GRANT EXECUTE ON PROCEDURE sp_generate_sustainability_report TO '${app_user}'@'%';

-- Grants. Os de supplier saem antes do DROP: o MySQL não revoga privilégio de tabela apagada.
GRANT SELECT, TRIGGER ON company TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON company_plan TO 'supply_audit'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON company TO '${app_user}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON company_plan TO '${app_user}'@'%';
GRANT SELECT ON plan TO '${app_user}'@'%';
REVOKE SELECT, TRIGGER ON supplier FROM 'supply_audit'@'%';
REVOKE SELECT, INSERT, UPDATE, DELETE ON supplier FROM '${app_user}'@'%';

-- O DROP leva junto as triggers audit_supplier_*.
DROP TABLE supplier;

-- Auditoria (RNF-02), no padrão da V5. As colunas geradas *_type e active_company_id ficam de fora.
DELIMITER $$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_insert
AFTER INSERT ON company FOR EACH ROW
CALL audit_write('company', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'name', NEW.name,
        'cnpj', NEW.cnpj,
        'address_id', NEW.address_id,
        'phone', NEW.phone,
        'email', NEW.email,
        'type', NEW.type,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_update
AFTER UPDATE ON company FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (BINARY OLD.name <=> BINARY NEW.name) THEN
        SET b = JSON_SET(b, '$.name', OLD.name), a = JSON_SET(a, '$.name', NEW.name);
    END IF;
    IF NOT (OLD.cnpj <=> NEW.cnpj) THEN
        SET b = JSON_SET(b, '$.cnpj', OLD.cnpj), a = JSON_SET(a, '$.cnpj', NEW.cnpj);
    END IF;
    IF NOT (OLD.address_id <=> NEW.address_id) THEN
        SET b = JSON_SET(b, '$.address_id', OLD.address_id), a = JSON_SET(a, '$.address_id', NEW.address_id);
    END IF;
    IF NOT (OLD.phone <=> NEW.phone) THEN
        SET b = JSON_SET(b, '$.phone', OLD.phone), a = JSON_SET(a, '$.phone', NEW.phone);
    END IF;
    IF NOT (OLD.email <=> NEW.email) THEN
        SET b = JSON_SET(b, '$.email', OLD.email), a = JSON_SET(a, '$.email', NEW.email);
    END IF;
    IF NOT (OLD.type <=> NEW.type) THEN
        SET b = JSON_SET(b, '$.type', OLD.type), a = JSON_SET(a, '$.type', NEW.type);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('company', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_delete
AFTER DELETE ON company FOR EACH ROW
CALL audit_write('company', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'name', OLD.name,
        'cnpj', OLD.cnpj,
        'address_id', OLD.address_id,
        'phone', OLD.phone,
        'email', OLD.email,
        'type', OLD.type,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_plan_insert
AFTER INSERT ON company_plan FOR EACH ROW
CALL audit_write('company_plan', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'company_id', NEW.company_id,
        'plan_id', NEW.plan_id,
        'started_at', audit_ts(NEW.started_at),
        'ended_at', audit_ts(NEW.ended_at),
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_plan_update
AFTER UPDATE ON company_plan FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.company_id <=> NEW.company_id) THEN
        SET b = JSON_SET(b, '$.company_id', OLD.company_id), a = JSON_SET(a, '$.company_id', NEW.company_id);
    END IF;
    IF NOT (OLD.plan_id <=> NEW.plan_id) THEN
        SET b = JSON_SET(b, '$.plan_id', OLD.plan_id), a = JSON_SET(a, '$.plan_id', NEW.plan_id);
    END IF;
    IF NOT (OLD.started_at <=> NEW.started_at) THEN
        SET b = JSON_SET(b, '$.started_at', audit_ts(OLD.started_at)), a = JSON_SET(a, '$.started_at', audit_ts(NEW.started_at));
    END IF;
    IF NOT (OLD.ended_at <=> NEW.ended_at) THEN
        SET b = JSON_SET(b, '$.ended_at', audit_ts(OLD.ended_at)), a = JSON_SET(a, '$.ended_at', audit_ts(NEW.ended_at));
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('company_plan', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_company_plan_delete
AFTER DELETE ON company_plan FOR EACH ROW
CALL audit_write('company_plan', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'company_id', OLD.company_id,
        'plan_id', OLD.plan_id,
        'started_at', audit_ts(OLD.started_at),
        'ended_at', audit_ts(OLD.ended_at),
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_users_insert
AFTER INSERT ON users FOR EACH ROW
CALL audit_write('users', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'name', NEW.name,
        'email', NEW.email,
        'role', NEW.role,
        'company_id', NEW.company_id,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_users_update
AFTER UPDATE ON users FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.name <=> NEW.name) THEN
        SET b = JSON_SET(b, '$.name', OLD.name), a = JSON_SET(a, '$.name', NEW.name);
    END IF;
    IF NOT (OLD.email <=> NEW.email) THEN
        SET b = JSON_SET(b, '$.email', OLD.email), a = JSON_SET(a, '$.email', NEW.email);
    END IF;
    IF NOT (OLD.role <=> NEW.role) THEN
        SET b = JSON_SET(b, '$.role', OLD.role), a = JSON_SET(a, '$.role', NEW.role);
    END IF;
    IF NOT (OLD.company_id <=> NEW.company_id) THEN
        SET b = JSON_SET(b, '$.company_id', OLD.company_id), a = JSON_SET(a, '$.company_id', NEW.company_id);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('users', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_users_delete
AFTER DELETE ON users FOR EACH ROW
CALL audit_write('users', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'name', OLD.name,
        'email', OLD.email,
        'role', OLD.role,
        'company_id', OLD.company_id,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_product_insert
AFTER INSERT ON product FOR EACH ROW
CALL audit_write('product', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'supplier_id', NEW.supplier_id,
        'name', NEW.name,
        'category', NEW.category,
        'unit', NEW.unit,
        'description', NEW.description,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_product_update
AFTER UPDATE ON product FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.supplier_id <=> NEW.supplier_id) THEN
        SET b = JSON_SET(b, '$.supplier_id', OLD.supplier_id), a = JSON_SET(a, '$.supplier_id', NEW.supplier_id);
    END IF;
    IF NOT (BINARY OLD.name <=> BINARY NEW.name) THEN
        SET b = JSON_SET(b, '$.name', OLD.name), a = JSON_SET(a, '$.name', NEW.name);
    END IF;
    IF NOT (OLD.category <=> NEW.category) THEN
        SET b = JSON_SET(b, '$.category', OLD.category), a = JSON_SET(a, '$.category', NEW.category);
    END IF;
    IF NOT (OLD.unit <=> NEW.unit) THEN
        SET b = JSON_SET(b, '$.unit', OLD.unit), a = JSON_SET(a, '$.unit', NEW.unit);
    END IF;
    IF NOT (BINARY OLD.description <=> BINARY NEW.description) THEN
        SET b = JSON_SET(b, '$.description', OLD.description), a = JSON_SET(a, '$.description', NEW.description);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('product', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_product_delete
AFTER DELETE ON product FOR EACH ROW
CALL audit_write('product', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'supplier_id', OLD.supplier_id,
        'name', OLD.name,
        'category', OLD.category,
        'unit', OLD.unit,
        'description', OLD.description,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_batch_insert
AFTER INSERT ON batch FOR EACH ROW
CALL audit_write('batch', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'product_id', NEW.product_id,
        'supplier_id', NEW.supplier_id,
        'market_id', NEW.market_id,
        'status', NEW.status,
        'rejection_reason', NEW.rejection_reason,
        'quantity', NEW.quantity,
        'produced_at', NEW.produced_at,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

-- Mudança de status vira STATUS_CHANGE, como em certification.
CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_batch_update
AFTER UPDATE ON batch FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.product_id <=> NEW.product_id) THEN
        SET b = JSON_SET(b, '$.product_id', OLD.product_id), a = JSON_SET(a, '$.product_id', NEW.product_id);
    END IF;
    IF NOT (OLD.supplier_id <=> NEW.supplier_id) THEN
        SET b = JSON_SET(b, '$.supplier_id', OLD.supplier_id), a = JSON_SET(a, '$.supplier_id', NEW.supplier_id);
    END IF;
    IF NOT (OLD.market_id <=> NEW.market_id) THEN
        SET b = JSON_SET(b, '$.market_id', OLD.market_id), a = JSON_SET(a, '$.market_id', NEW.market_id);
    END IF;
    IF NOT (OLD.status <=> NEW.status) THEN
        SET b = JSON_SET(b, '$.status', OLD.status), a = JSON_SET(a, '$.status', NEW.status);
    END IF;
    IF NOT (OLD.rejection_reason <=> NEW.rejection_reason) THEN
        SET b = JSON_SET(b, '$.rejection_reason', OLD.rejection_reason), a = JSON_SET(a, '$.rejection_reason', NEW.rejection_reason);
    END IF;
    IF NOT (OLD.quantity <=> NEW.quantity) THEN
        SET b = JSON_SET(b, '$.quantity', OLD.quantity), a = JSON_SET(a, '$.quantity', NEW.quantity);
    END IF;
    IF NOT (OLD.produced_at <=> NEW.produced_at) THEN
        SET b = JSON_SET(b, '$.produced_at', OLD.produced_at), a = JSON_SET(a, '$.produced_at', NEW.produced_at);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('batch', NEW.id, 'UPDATE', b, a, NOT (OLD.status <=> NEW.status));
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_batch_delete
AFTER DELETE ON batch FOR EACH ROW
CALL audit_write('batch', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'product_id', OLD.product_id,
        'supplier_id', OLD.supplier_id,
        'market_id', OLD.market_id,
        'status', OLD.status,
        'rejection_reason', OLD.rejection_reason,
        'quantity', OLD.quantity,
        'produced_at', OLD.produced_at,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_report_insert
AFTER INSERT ON report FOR EACH ROW
CALL audit_write('report', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'supplier_id', NEW.supplier_id,
        'market_id', NEW.market_id,
        'period_start_at', NEW.period_start_at,
        'period_end_at', NEW.period_end_at,
        'total_co2_kg', NEW.total_co2_kg,
        'tracked_product_count', NEW.tracked_product_count,
        'batch_count', NEW.batch_count,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_report_update
AFTER UPDATE ON report FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.supplier_id <=> NEW.supplier_id) THEN
        SET b = JSON_SET(b, '$.supplier_id', OLD.supplier_id), a = JSON_SET(a, '$.supplier_id', NEW.supplier_id);
    END IF;
    IF NOT (OLD.market_id <=> NEW.market_id) THEN
        SET b = JSON_SET(b, '$.market_id', OLD.market_id), a = JSON_SET(a, '$.market_id', NEW.market_id);
    END IF;
    IF NOT (OLD.period_start_at <=> NEW.period_start_at) THEN
        SET b = JSON_SET(b, '$.period_start_at', OLD.period_start_at), a = JSON_SET(a, '$.period_start_at', NEW.period_start_at);
    END IF;
    IF NOT (OLD.period_end_at <=> NEW.period_end_at) THEN
        SET b = JSON_SET(b, '$.period_end_at', OLD.period_end_at), a = JSON_SET(a, '$.period_end_at', NEW.period_end_at);
    END IF;
    IF NOT (OLD.total_co2_kg <=> NEW.total_co2_kg) THEN
        SET b = JSON_SET(b, '$.total_co2_kg', OLD.total_co2_kg), a = JSON_SET(a, '$.total_co2_kg', NEW.total_co2_kg);
    END IF;
    IF NOT (OLD.tracked_product_count <=> NEW.tracked_product_count) THEN
        SET b = JSON_SET(b, '$.tracked_product_count', OLD.tracked_product_count), a = JSON_SET(a, '$.tracked_product_count', NEW.tracked_product_count);
    END IF;
    IF NOT (OLD.batch_count <=> NEW.batch_count) THEN
        SET b = JSON_SET(b, '$.batch_count', OLD.batch_count), a = JSON_SET(a, '$.batch_count', NEW.batch_count);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('report', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_report_delete
AFTER DELETE ON report FOR EACH ROW
CALL audit_write('report', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'supplier_id', OLD.supplier_id,
        'market_id', OLD.market_id,
        'period_start_at', OLD.period_start_at,
        'period_end_at', OLD.period_end_at,
        'total_co2_kg', OLD.total_co2_kg,
        'tracked_product_count', OLD.tracked_product_count,
        'batch_count', OLD.batch_count,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

DELIMITER ;
