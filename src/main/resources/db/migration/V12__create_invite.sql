-- Feature 011: convite de usuário por email. Sem status: a linha existe quando o email foi enviado,
-- expirado é expires_at <= NOW(6) e aceito é accepted. A companhia vem de invited_by -> users.company_id.
-- O link leva o token; aqui fica só o SHA-256 dele (hex).

CREATE TABLE invite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    invited_by  BIGINT       NOT NULL,
    invited_for VARCHAR(180) NOT NULL,
    role        ENUM('ADMIN', 'MANAGER', 'AUDITOR', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci NOT NULL,
    token_hash  CHAR(64)     NOT NULL,
    accepted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    -- RN-06: 24h por construção, ninguém grava expires_at.
    expires_at  DATETIME(6) GENERATED ALWAYS AS (created_at + INTERVAL 24 HOUR) STORED,
    CONSTRAINT uq_invite_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_invite_invited_by FOREIGN KEY (invited_by) REFERENCES users (id),
    INDEX idx_invite_invited_for (invited_for, accepted, expires_at),
    INDEX idx_invite_invited_by (invited_by, accepted, expires_at)
);

-- A app nunca apaga convite.
GRANT SELECT, TRIGGER ON invite TO 'supply_audit'@'%';
GRANT SELECT, INSERT, UPDATE ON invite TO '${app_user}'@'%';

-- Auditoria (RNF-02), no padrão da V5. token_hash fica de fora (é credencial) e expires_at
-- também, por ser gerada a partir de created_at.
DELIMITER $$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_invite_insert
AFTER INSERT ON invite FOR EACH ROW
CALL audit_write('invite', NEW.id, 'INSERT', NULL, JSON_OBJECT(
        'id', NEW.id,
        'invited_by', NEW.invited_by,
        'invited_for', NEW.invited_for,
        'role', NEW.role,
        'accepted', NEW.accepted,
        'created_at', audit_ts(NEW.created_at),
        'updated_at', audit_ts(NEW.updated_at)), FALSE)$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_invite_update
AFTER UPDATE ON invite FOR EACH ROW
BEGIN
    DECLARE b JSON DEFAULT JSON_OBJECT();
    DECLARE a JSON DEFAULT JSON_OBJECT();
    IF NOT (OLD.id <=> NEW.id) THEN
        SET b = JSON_SET(b, '$.id', OLD.id), a = JSON_SET(a, '$.id', NEW.id);
    END IF;
    IF NOT (OLD.invited_by <=> NEW.invited_by) THEN
        SET b = JSON_SET(b, '$.invited_by', OLD.invited_by), a = JSON_SET(a, '$.invited_by', NEW.invited_by);
    END IF;
    IF NOT (OLD.invited_for <=> NEW.invited_for) THEN
        SET b = JSON_SET(b, '$.invited_for', OLD.invited_for), a = JSON_SET(a, '$.invited_for', NEW.invited_for);
    END IF;
    IF NOT (OLD.role <=> NEW.role) THEN
        SET b = JSON_SET(b, '$.role', OLD.role), a = JSON_SET(a, '$.role', NEW.role);
    END IF;
    IF NOT (OLD.accepted <=> NEW.accepted) THEN
        SET b = JSON_SET(b, '$.accepted', OLD.accepted), a = JSON_SET(a, '$.accepted', NEW.accepted);
    END IF;
    IF NOT (OLD.created_at <=> NEW.created_at) THEN
        SET b = JSON_SET(b, '$.created_at', audit_ts(OLD.created_at)), a = JSON_SET(a, '$.created_at', audit_ts(NEW.created_at));
    END IF;
    CALL audit_write('invite', NEW.id, 'UPDATE', b, a, FALSE);
END$$

CREATE DEFINER = 'supply_audit'@'%' TRIGGER audit_invite_delete
AFTER DELETE ON invite FOR EACH ROW
CALL audit_write('invite', OLD.id, 'DELETE', JSON_OBJECT(
        'id', OLD.id,
        'invited_by', OLD.invited_by,
        'invited_for', OLD.invited_for,
        'role', OLD.role,
        'accepted', OLD.accepted,
        'created_at', audit_ts(OLD.created_at),
        'updated_at', audit_ts(OLD.updated_at)), NULL, FALSE)$$

DELIMITER ;
