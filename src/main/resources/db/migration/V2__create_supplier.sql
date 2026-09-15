CREATE TABLE supplier (
    supplier_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    cnpj CHAR(14) NOT NULL UNIQUE,
    address_id BIGINT NOT NULL,
    phone VARCHAR(30),
    registered_at DATE NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT fk_supplier_address
        FOREIGN KEY (address_id)
        REFERENCES address (address_id)
);

CREATE INDEX idx_supplier_address_id ON supplier (address_id);
CREATE INDEX idx_supplier_name ON supplier (name);
