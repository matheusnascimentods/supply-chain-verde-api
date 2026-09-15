CREATE TYPE certification_status AS ENUM (
    'ACTIVE',
    'EXPIRED',
    'SUSPENDED',
    'UNDER_REVIEW'
);

CREATE TABLE certification (
    certification_id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    certification VARCHAR(180) NOT NULL,
    issuing_body VARCHAR(180) NOT NULL,
    issued_at DATE NOT NULL,
    expires_at DATE NOT NULL,
    status certification_status NOT NULL,
    CONSTRAINT fk_certification_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES supplier (supplier_id),
    CONSTRAINT chk_certification_dates
        CHECK (expires_at >= issued_at)
);

CREATE INDEX idx_certification_supplier_id ON certification (supplier_id);
CREATE INDEX idx_certification_status ON certification (status);
CREATE INDEX idx_certification_expires_at ON certification (expires_at);
