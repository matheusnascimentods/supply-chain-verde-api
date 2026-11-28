CREATE TABLE certification (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id   BIGINT NOT NULL,
    certification VARCHAR(180) NOT NULL,
    issuing_body  VARCHAR(180) NOT NULL,
    issued_at     DATE NOT NULL,
    expires_at    DATE NOT NULL,
    status        ENUM('ACTIVE', 'EXPIRED', 'SUSPENDED', 'UNDER_REVIEW') COLLATE utf8mb4_0900_as_ci NOT NULL,
    supplier_type ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
                  GENERATED ALWAYS AS ('SUPPLIER') STORED,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_certification_dates CHECK (expires_at >= issued_at),
    CONSTRAINT fk_certification_supplier FOREIGN KEY (supplier_id, supplier_type) REFERENCES company (id, type),
    INDEX idx_certification_expires_at (expires_at),
    INDEX idx_certification_status (status),
    INDEX idx_certification_supplier_id (supplier_id)
);
