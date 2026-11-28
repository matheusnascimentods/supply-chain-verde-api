CREATE TABLE invite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    invited_by  BIGINT NOT NULL,
    invited_for VARCHAR(180) NOT NULL,
    role        ENUM('ADMIN', 'MANAGER', 'AUDITOR', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci NOT NULL,
    token_hash  CHAR(64) NOT NULL,
    accepted    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    expires_at  DATETIME(6) GENERATED ALWAYS AS (created_at + INTERVAL 24 HOUR) STORED,
    CONSTRAINT uq_invite_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_invite_invited_by FOREIGN KEY (invited_by) REFERENCES users (id),
    INDEX idx_invite_invited_for (invited_for, accepted, expires_at),
    INDEX idx_invite_invited_by (invited_by, accepted, expires_at)
);
