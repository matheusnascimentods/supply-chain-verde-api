CREATE TYPE stage_type AS ENUM (
    'PRODUCTION',
    'STORAGE',
    'PROCESSING',
    'TRANSPORT',
    'DISTRIBUTION',
    'RETAIL'
);

CREATE TABLE chain (
    chain_id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    origin_address_id BIGINT,
    destination_address_id BIGINT,
    responsible_user_id BIGINT NOT NULL,
    stage_type stage_type NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    CONSTRAINT fk_chain_batch
        FOREIGN KEY (batch_id)
        REFERENCES batch (batch_id),
    CONSTRAINT fk_chain_origin_address
        FOREIGN KEY (origin_address_id)
        REFERENCES address (address_id),
    CONSTRAINT fk_chain_destination_address
        FOREIGN KEY (destination_address_id)
        REFERENCES address (address_id),
    CONSTRAINT fk_chain_responsible_user
        FOREIGN KEY (responsible_user_id)
        REFERENCES users (user_id),
    CONSTRAINT chk_chain_dates
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_chain_batch_id ON chain (batch_id);
CREATE INDEX idx_chain_origin_address_id ON chain (origin_address_id);
CREATE INDEX idx_chain_destination_address_id ON chain (destination_address_id);
CREATE INDEX idx_chain_responsible_user_id ON chain (responsible_user_id);
CREATE INDEX idx_chain_started_at ON chain (started_at);
