CREATE TABLE batch (
    batch_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    produced_at DATE NOT NULL,
    CONSTRAINT fk_batch_product
        FOREIGN KEY (product_id)
        REFERENCES product (product_id),
    CONSTRAINT fk_batch_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES supplier (supplier_id),
    CONSTRAINT chk_batch_quantity_positive
        CHECK (quantity > 0)
);

CREATE INDEX idx_batch_product_id ON batch (product_id);
CREATE INDEX idx_batch_supplier_id ON batch (supplier_id);
CREATE INDEX idx_batch_produced_at ON batch (produced_at);
