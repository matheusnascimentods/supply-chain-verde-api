CREATE TABLE report (
    report_id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    period_start_at DATE NOT NULL,
    period_end_at DATE NOT NULL,
    total_co2_kg NUMERIC(19, 4) NOT NULL,
    tracked_product_count INTEGER NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES supplier (supplier_id),
    CONSTRAINT chk_report_period
        CHECK (period_end_at >= period_start_at),
    CONSTRAINT chk_report_total_co2_non_negative
        CHECK (total_co2_kg >= 0),
    CONSTRAINT chk_report_tracked_product_count_non_negative
        CHECK (tracked_product_count >= 0)
);

CREATE INDEX idx_report_supplier_id ON report (supplier_id);
CREATE INDEX idx_report_period ON report (period_start_at, period_end_at);
