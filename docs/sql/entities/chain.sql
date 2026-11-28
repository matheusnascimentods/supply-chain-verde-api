CREATE TABLE chain (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id               BIGINT NOT NULL,
    destination_address_id BIGINT NOT NULL,
    responsible_user_id    BIGINT NOT NULL,
    stage_type             ENUM('PRODUCTION', 'STORAGE', 'PROCESSING', 'TRANSPORT', 'DISTRIBUTION', 'RETAIL')
                           COLLATE utf8mb4_0900_as_ci NOT NULL,
    started_at             DATETIME(6) NOT NULL,
    ended_at               DATETIME(6) NULL,
    created_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    previous_stage_type    ENUM('PRODUCTION', 'STORAGE', 'PROCESSING', 'TRANSPORT', 'DISTRIBUTION', 'RETAIL')
                           COLLATE utf8mb4_0900_as_ci
                           GENERATED ALWAYS AS (CASE stage_type
                               WHEN 'STORAGE' THEN 'PRODUCTION'
                               WHEN 'PROCESSING' THEN 'STORAGE'
                               WHEN 'TRANSPORT' THEN 'PROCESSING'
                               WHEN 'DISTRIBUTION' THEN 'TRANSPORT'
                               WHEN 'RETAIL' THEN 'DISTRIBUTION'
                           END) STORED,
    CONSTRAINT chk_chain_dates CHECK (ended_at IS NULL OR ended_at >= started_at),
    CONSTRAINT uq_chain_batch_stage UNIQUE (batch_id, stage_type),
    CONSTRAINT fk_chain_batch FOREIGN KEY (batch_id) REFERENCES batch (id),
    CONSTRAINT fk_chain_destination_address FOREIGN KEY (destination_address_id) REFERENCES address (id),
    CONSTRAINT fk_chain_responsible_user FOREIGN KEY (responsible_user_id) REFERENCES users (id),
    CONSTRAINT fk_chain_previous_stage FOREIGN KEY (batch_id, previous_stage_type)
        REFERENCES chain (batch_id, stage_type),
    INDEX idx_chain_batch_id (batch_id),
    INDEX idx_chain_destination_address_id (destination_address_id),
    INDEX idx_chain_responsible_user_id (responsible_user_id),
    INDEX idx_chain_started_at (started_at)
);
