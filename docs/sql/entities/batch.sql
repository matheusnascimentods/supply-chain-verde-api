CREATE TABLE batch (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    supplier_id     BIGINT NOT NULL,
    market_id       BIGINT NOT NULL,
    status          ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'REJECTED', 'CANCELED') COLLATE utf8mb4_0900_as_ci NOT NULL,
    rejection_reason VARCHAR(500) NULL,
    quantity        DECIMAL(19,4) NOT NULL,
    produced_at     DATE NULL,
    market_type     ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
                    GENERATED ALWAYS AS ('MARKET') STORED,
    supplier_type   ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
                    GENERATED ALWAYS AS ('SUPPLIER') STORED,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_batch_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_batch_rejection_reason CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL)),
    CONSTRAINT chk_batch_produced_at CHECK (status NOT IN ('ACTIVE', 'COMPLETED') OR produced_at IS NOT NULL),
    CONSTRAINT fk_batch_market FOREIGN KEY (market_id, market_type) REFERENCES company (id, type),
    CONSTRAINT fk_batch_supplier FOREIGN KEY (supplier_id, supplier_type) REFERENCES company (id, type),
    CONSTRAINT fk_batch_product_supplier FOREIGN KEY (product_id, supplier_id) REFERENCES product (id, supplier_id),
    INDEX idx_batch_produced_at (produced_at),
    INDEX idx_batch_product_id (product_id),
    INDEX idx_batch_supplier_id (supplier_id),
    INDEX idx_batch_market_id (market_id),
    INDEX idx_batch_status (status),
    INDEX idx_batch_market_status_created (market_id, status, created_at)
);
