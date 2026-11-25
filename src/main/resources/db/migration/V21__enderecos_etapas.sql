-- Feature 018: a origem da etapa deixa de ser gravada e passa a ser derivada (RN-04): a PRODUCTION parte do
-- fornecedor e as demais etapas partem do destino da anterior. O destino passa a ser obrigatório (RN-01).

-- RN-07: destino nulo = o lote ficou no lugar (origem antiga) ou, sem ela, o fornecedor.
-- ponytail: não percorre a cadeia; se origem e destino forem nulos, cai no fornecedor mesmo que a etapa anterior
-- tenha terminado em outro lugar. Só afeta etapas da UI antiga registradas com "Não informar".
UPDATE chain c
JOIN batch b ON b.id = c.batch_id
JOIN company s ON s.id = b.supplier_id
SET c.destination_address_id = COALESCE(c.origin_address_id, s.address_id)
WHERE c.destination_address_id IS NULL
  AND c.stage_type <> 'RETAIL';

-- RN-08: o varejo termina no endereço do mercado do lote (RN-03).
UPDATE chain c
JOIN batch b ON b.id = c.batch_id
JOIN company m ON m.id = b.market_id
SET c.destination_address_id = m.address_id
WHERE c.stage_type = 'RETAIL'
  AND NOT (c.destination_address_id <=> m.address_id);

-- RN-09: as triggers citam origin_address_id, então saem antes da coluna e voltam sem ela.
DROP TRIGGER audit_chain_insert;
DROP TRIGGER audit_chain_update;
DROP TRIGGER audit_chain_delete;

-- O MySQL não muda a nulidade de coluna com FK (erro 1832): a FK do destino sai e volta em outro ALTER,
-- porque no mesmo comando o nome repetido conflita.
ALTER TABLE chain
    DROP FOREIGN KEY fk_chain_origin_address,
    DROP FOREIGN KEY fk_chain_destination_address,
    DROP INDEX idx_chain_origin_address_id,
    DROP COLUMN origin_address_id,
    MODIFY destination_address_id BIGINT NOT NULL;

ALTER TABLE chain
    ADD CONSTRAINT fk_chain_destination_address FOREIGN KEY (destination_address_id) REFERENCES address (id);

DELIMITER $$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_chain_insert
AFTER INSERT ON chain FOR EACH ROW
CALL audit_write('chain', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'batch_id', NEW.batch_id,
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
        'destination_address_id', OLD.destination_address_id,
        'responsible_user_id', OLD.responsible_user_id,
        'stage_type', OLD.stage_type,
        'started_at', audit_ts(OLD.started_at),
        'ended_at', audit_ts(OLD.ended_at),
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at),
        'previous_stage_type', OLD.previous_stage_type), NULL, FALSE)$$

DELIMITER ;
