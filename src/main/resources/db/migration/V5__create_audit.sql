-- Auditoria por trigger (ADR 0001/0002). Vem depois do seed, então o seed não gera eventos (RN-07).
-- Quem escreve em audit_log é supply_audit, DEFINER das triggers e de audit_write; a app só lê (V6).
-- O ator vem de @app_user_id, definido pelo AuditActorContextAspect antes de cada escrita e limpo ao fim da transação.
CREATE TABLE audit_log (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT NULL,
    action             ENUM('INSERT', 'UPDATE', 'DELETE', 'STATUS_CHANGE') COLLATE utf8mb4_0900_as_ci NOT NULL,
    affected_table     VARCHAR(120) NOT NULL,
    affected_entity_id BIGINT NOT NULL,
    before_data        JSON NULL,
    after_data         JSON NULL,
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_audit_log_user_id (user_id),
    INDEX idx_audit_log_performed_at (created_at DESC, id DESC),
    INDEX idx_audit_log_action (action),
    INDEX idx_audit_log_affected_entity (affected_table, affected_entity_id)
);

GRANT INSERT ON audit_log TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON address TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON batch TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON carbon_emission TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON certification TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON chain TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON product TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON report TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON supplier TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON transport TO 'supply_audit'@'%';
GRANT SELECT, TRIGGER ON users TO 'supply_audit'@'%';

DELIMITER $$

-- Mesmo formato ISO 8601 que o to_jsonb do PostgreSQL dava para TIMESTAMPTZ. São Paulo não tem horário de verão.
CREATE FUNCTION audit_ts(p_value DATETIME(6)) RETURNS VARCHAR(40)
DETERMINISTIC NO SQL
RETURN DATE_FORMAT(p_value, '%Y-%m-%dT%H:%i:%s.%f-03:00')$$

-- Parte comum das 30 triggers. Roda na mesma transação da escrita: um erro aqui reverte a instrução (RN-01).
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

GRANT EXECUTE ON FUNCTION audit_ts TO 'supply_audit'@'%';
GRANT EXECUTE ON PROCEDURE audit_write TO 'supply_audit'@'%';

DELIMITER $$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_address_insert
AFTER INSERT ON address FOR EACH ROW
CALL audit_write('address', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'street', NEW.street,
        'number', NEW.number,
        'neighborhood', NEW.neighborhood,
        'complement', NEW.complement,
        'zip_code', NEW.zip_code,
        'city', NEW.city,
        'state', NEW.state,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_address_update
AFTER UPDATE ON address FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.street <=> NEW.street) THEN
        SET b = JSON_SET(b, '$.street', OLD.street), a = JSON_SET(a, '$.street', NEW.street);
    END IF;
    IF NOT (OLD.number <=> NEW.number) THEN
        SET b = JSON_SET(b, '$.number', OLD.number), a = JSON_SET(a, '$.number', NEW.number);
    END IF;
    IF NOT (OLD.neighborhood <=> NEW.neighborhood) THEN
        SET b = JSON_SET(b, '$.neighborhood', OLD.neighborhood), a = JSON_SET(a, '$.neighborhood', NEW.neighborhood);
    END IF;
    IF NOT (OLD.complement <=> NEW.complement) THEN
        SET b = JSON_SET(b, '$.complement', OLD.complement), a = JSON_SET(a, '$.complement', NEW.complement);
    END IF;
    IF NOT (OLD.zip_code <=> NEW.zip_code) THEN
        SET b = JSON_SET(b, '$.zip_code', OLD.zip_code), a = JSON_SET(a, '$.zip_code', NEW.zip_code);
    END IF;
    IF NOT (OLD.city <=> NEW.city) THEN
        SET b = JSON_SET(b, '$.city', OLD.city), a = JSON_SET(a, '$.city', NEW.city);
    END IF;
    IF NOT (OLD.state <=> NEW.state) THEN
        SET b = JSON_SET(b, '$.state', OLD.state), a = JSON_SET(a, '$.state', NEW.state);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('address', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_address_delete
AFTER DELETE ON address FOR EACH ROW
CALL audit_write('address', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'street', OLD.street,
        'number', OLD.number,
        'neighborhood', OLD.neighborhood,
        'complement', OLD.complement,
        'zip_code', OLD.zip_code,
        'city', OLD.city,
        'state', OLD.state,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_batch_insert
AFTER INSERT ON batch FOR EACH ROW
CALL audit_write('batch', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'product_id', NEW.product_id,
        'supplier_id', NEW.supplier_id,
        'quantity', NEW.quantity,
        'produced_at', NEW.produced_at,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

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
    IF NOT (OLD.quantity <=> NEW.quantity) THEN
        SET b = JSON_SET(b, '$.quantity', OLD.quantity), a = JSON_SET(a, '$.quantity', NEW.quantity);
    END IF;
    IF NOT (OLD.produced_at <=> NEW.produced_at) THEN
        SET b = JSON_SET(b, '$.produced_at', OLD.produced_at), a = JSON_SET(a, '$.produced_at', NEW.produced_at);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('batch', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_batch_delete
AFTER DELETE ON batch FOR EACH ROW
CALL audit_write('batch', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'product_id', OLD.product_id,
        'supplier_id', OLD.supplier_id,
        'quantity', OLD.quantity,
        'produced_at', OLD.produced_at,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_carbon_emission_insert
AFTER INSERT ON carbon_emission FOR EACH ROW
CALL audit_write('carbon_emission', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'chain_id', NEW.chain_id,
        'emission_factor', NEW.emission_factor,
        'co2_kg', NEW.co2_kg,
        'calculation_method', NEW.calculation_method,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_carbon_emission_update
AFTER UPDATE ON carbon_emission FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.chain_id <=> NEW.chain_id) THEN
        SET b = JSON_SET(b, '$.chain_id', OLD.chain_id), a = JSON_SET(a, '$.chain_id', NEW.chain_id);
    END IF;
    IF NOT (OLD.emission_factor <=> NEW.emission_factor) THEN
        SET b = JSON_SET(b, '$.emission_factor', OLD.emission_factor), a = JSON_SET(a, '$.emission_factor', NEW.emission_factor);
    END IF;
    IF NOT (OLD.co2_kg <=> NEW.co2_kg) THEN
        SET b = JSON_SET(b, '$.co2_kg', OLD.co2_kg), a = JSON_SET(a, '$.co2_kg', NEW.co2_kg);
    END IF;
    IF NOT (OLD.calculation_method <=> NEW.calculation_method) THEN
        SET b = JSON_SET(b, '$.calculation_method', OLD.calculation_method), a = JSON_SET(a, '$.calculation_method', NEW.calculation_method);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('carbon_emission', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_carbon_emission_delete
AFTER DELETE ON carbon_emission FOR EACH ROW
CALL audit_write('carbon_emission', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'chain_id', OLD.chain_id,
        'emission_factor', OLD.emission_factor,
        'co2_kg', OLD.co2_kg,
        'calculation_method', OLD.calculation_method,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_certification_insert
AFTER INSERT ON certification FOR EACH ROW
CALL audit_write('certification', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'supplier_id', NEW.supplier_id,
        'certification', NEW.certification,
        'issuing_body', NEW.issuing_body,
        'issued_at', NEW.issued_at,
        'expires_at', NEW.expires_at,
        'status', NEW.status,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_certification_update
AFTER UPDATE ON certification FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.supplier_id <=> NEW.supplier_id) THEN
        SET b = JSON_SET(b, '$.supplier_id', OLD.supplier_id), a = JSON_SET(a, '$.supplier_id', NEW.supplier_id);
    END IF;
    IF NOT (OLD.certification <=> NEW.certification) THEN
        SET b = JSON_SET(b, '$.certification', OLD.certification), a = JSON_SET(a, '$.certification', NEW.certification);
    END IF;
    IF NOT (OLD.issuing_body <=> NEW.issuing_body) THEN
        SET b = JSON_SET(b, '$.issuing_body', OLD.issuing_body), a = JSON_SET(a, '$.issuing_body', NEW.issuing_body);
    END IF;
    IF NOT (OLD.issued_at <=> NEW.issued_at) THEN
        SET b = JSON_SET(b, '$.issued_at', OLD.issued_at), a = JSON_SET(a, '$.issued_at', NEW.issued_at);
    END IF;
    IF NOT (OLD.expires_at <=> NEW.expires_at) THEN
        SET b = JSON_SET(b, '$.expires_at', OLD.expires_at), a = JSON_SET(a, '$.expires_at', NEW.expires_at);
    END IF;
    IF NOT (OLD.status <=> NEW.status) THEN
        SET b = JSON_SET(b, '$.status', OLD.status), a = JSON_SET(a, '$.status', NEW.status);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('certification', NEW.id, 'UPDATE', b, a, NOT (OLD.status <=> NEW.status));
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_certification_delete
AFTER DELETE ON certification FOR EACH ROW
CALL audit_write('certification', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'supplier_id', OLD.supplier_id,
        'certification', OLD.certification,
        'issuing_body', OLD.issuing_body,
        'issued_at', OLD.issued_at,
        'expires_at', OLD.expires_at,
        'status', OLD.status,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_chain_insert
AFTER INSERT ON chain FOR EACH ROW
CALL audit_write('chain', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'batch_id', NEW.batch_id,
        'origin_address_id', NEW.origin_address_id,
        'destination_address_id', NEW.destination_address_id,
        'responsible_user_id', NEW.responsible_user_id,
        'stage_type', NEW.stage_type,
        'started_at', audit_ts(NEW.started_at),
        'ended_at', audit_ts(NEW.ended_at),
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at),
        'previous_stage_type', NEW.previous_stage_type), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_chain_update
AFTER UPDATE ON chain FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.batch_id <=> NEW.batch_id) THEN
        SET b = JSON_SET(b, '$.batch_id', OLD.batch_id), a = JSON_SET(a, '$.batch_id', NEW.batch_id);
    END IF;
    IF NOT (OLD.origin_address_id <=> NEW.origin_address_id) THEN
        SET b = JSON_SET(b, '$.origin_address_id', OLD.origin_address_id), a = JSON_SET(a, '$.origin_address_id', NEW.origin_address_id);
    END IF;
    IF NOT (OLD.destination_address_id <=> NEW.destination_address_id) THEN
        SET b = JSON_SET(b, '$.destination_address_id', OLD.destination_address_id), a = JSON_SET(a, '$.destination_address_id', NEW.destination_address_id);
    END IF;
    IF NOT (OLD.responsible_user_id <=> NEW.responsible_user_id) THEN
        SET b = JSON_SET(b, '$.responsible_user_id', OLD.responsible_user_id), a = JSON_SET(a, '$.responsible_user_id', NEW.responsible_user_id);
    END IF;
    IF NOT (OLD.stage_type <=> NEW.stage_type) THEN
        SET b = JSON_SET(b, '$.stage_type', OLD.stage_type), a = JSON_SET(a, '$.stage_type', NEW.stage_type);
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
    IF NOT (OLD.previous_stage_type <=> NEW.previous_stage_type) THEN
        SET b = JSON_SET(b, '$.previous_stage_type', OLD.previous_stage_type), a = JSON_SET(a, '$.previous_stage_type', NEW.previous_stage_type);
    END IF;
    CALL audit_write('chain', NEW.id, 'UPDATE', b, a, NOT (OLD.stage_type <=> NEW.stage_type));
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_chain_delete
AFTER DELETE ON chain FOR EACH ROW
CALL audit_write('chain', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'batch_id', OLD.batch_id,
        'origin_address_id', OLD.origin_address_id,
        'destination_address_id', OLD.destination_address_id,
        'responsible_user_id', OLD.responsible_user_id,
        'stage_type', OLD.stage_type,
        'started_at', audit_ts(OLD.started_at),
        'ended_at', audit_ts(OLD.ended_at),
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at),
        'previous_stage_type', OLD.previous_stage_type), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_product_insert
AFTER INSERT ON product FOR EACH ROW
CALL audit_write('product', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
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
        'name', OLD.name,
        'category', OLD.category,
        'unit', OLD.unit,
        'description', OLD.description,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_report_insert
AFTER INSERT ON report FOR EACH ROW
CALL audit_write('report', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'supplier_id', NEW.supplier_id,
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
        'period_start_at', OLD.period_start_at,
        'period_end_at', OLD.period_end_at,
        'total_co2_kg', OLD.total_co2_kg,
        'tracked_product_count', OLD.tracked_product_count,
        'batch_count', OLD.batch_count,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_supplier_insert
AFTER INSERT ON supplier FOR EACH ROW
CALL audit_write('supplier', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'name', NEW.name,
        'cnpj', NEW.cnpj,
        'address_id', NEW.address_id,
        'phone', NEW.phone,
        'user_id', NEW.user_id,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_supplier_update
AFTER UPDATE ON supplier FOR EACH ROW
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
    IF NOT (OLD.user_id <=> NEW.user_id) THEN
        SET b = JSON_SET(b, '$.user_id', OLD.user_id), a = JSON_SET(a, '$.user_id', NEW.user_id);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('supplier', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_supplier_delete
AFTER DELETE ON supplier FOR EACH ROW
CALL audit_write('supplier', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'name', OLD.name,
        'cnpj', OLD.cnpj,
        'address_id', OLD.address_id,
        'phone', OLD.phone,
        'user_id', OLD.user_id,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_transport_insert
AFTER INSERT ON transport FOR EACH ROW
CALL audit_write('transport', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'chain_id', NEW.chain_id,
        'transport_mode', NEW.transport_mode,
        'distance', NEW.distance,
        'fuel_type', NEW.fuel_type,
        'capacity', NEW.capacity,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_transport_update
AFTER UPDATE ON transport FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.chain_id <=> NEW.chain_id) THEN
        SET b = JSON_SET(b, '$.chain_id', OLD.chain_id), a = JSON_SET(a, '$.chain_id', NEW.chain_id);
    END IF;
    IF NOT (OLD.transport_mode <=> NEW.transport_mode) THEN
        SET b = JSON_SET(b, '$.transport_mode', OLD.transport_mode), a = JSON_SET(a, '$.transport_mode', NEW.transport_mode);
    END IF;
    IF NOT (OLD.distance <=> NEW.distance) THEN
        SET b = JSON_SET(b, '$.distance', OLD.distance), a = JSON_SET(a, '$.distance', NEW.distance);
    END IF;
    IF NOT (OLD.fuel_type <=> NEW.fuel_type) THEN
        SET b = JSON_SET(b, '$.fuel_type', OLD.fuel_type), a = JSON_SET(a, '$.fuel_type', NEW.fuel_type);
    END IF;
    IF NOT (OLD.capacity <=> NEW.capacity) THEN
        SET b = JSON_SET(b, '$.capacity', OLD.capacity), a = JSON_SET(a, '$.capacity', NEW.capacity);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('transport', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_transport_delete
AFTER DELETE ON transport FOR EACH ROW
CALL audit_write('transport', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'chain_id', OLD.chain_id,
        'transport_mode', OLD.transport_mode,
        'distance', OLD.distance,
        'fuel_type', OLD.fuel_type,
        'capacity', OLD.capacity,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_users_insert
AFTER INSERT ON users FOR EACH ROW
CALL audit_write('users', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'name', NEW.name,
        'email', NEW.email,
        'role', NEW.role,
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
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

DELIMITER ;
