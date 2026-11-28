CREATE TABLE report (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id           BIGINT NOT NULL,
    market_id             BIGINT NOT NULL,
    period_start_at       DATE NOT NULL,
    period_end_at         DATE NOT NULL,
    total_co2_kg          DECIMAL(19,4) NOT NULL,
    tracked_product_count INT NOT NULL,
    batch_count           INT NOT NULL DEFAULT 0,
    market_type           ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
                          GENERATED ALWAYS AS ('MARKET') STORED,
    supplier_type         ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
                          GENERATED ALWAYS AS ('SUPPLIER') STORED,
    created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_report_batch_count_non_negative CHECK (batch_count >= 0),
    CONSTRAINT chk_report_period CHECK (period_end_at >= period_start_at),
    CONSTRAINT chk_report_total_co2_non_negative CHECK (total_co2_kg >= 0),
    CONSTRAINT chk_report_tracked_product_count_non_negative CHECK (tracked_product_count >= 0),
    CONSTRAINT fk_report_market FOREIGN KEY (market_id, market_type) REFERENCES company (id, type),
    CONSTRAINT fk_report_supplier FOREIGN KEY (supplier_id, supplier_type) REFERENCES company (id, type),
    INDEX idx_report_period (period_start_at, period_end_at),
    INDEX idx_report_supplier_id (supplier_id),
    INDEX idx_report_market_id (market_id)
);
