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
