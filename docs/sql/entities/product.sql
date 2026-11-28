CREATE TABLE product (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id   BIGINT NOT NULL,
    name          VARCHAR(180) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    category      ENUM('AGRICULTURE', 'LIVESTOCK', 'PROCESSED_FOOD', 'TEXTILE', 'FORESTRY', 'OTHER')
                  COLLATE utf8mb4_0900_as_ci NOT NULL,
    unit          ENUM('KG', 'TON', 'LITER', 'UNIT', 'M3') COLLATE utf8mb4_0900_as_ci NOT NULL,
    description   TEXT COLLATE utf8mb4_0900_ai_ci NULL,
    supplier_type ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
                  GENERATED ALWAYS AS ('SUPPLIER') STORED,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_product_id_supplier UNIQUE (id, supplier_id),
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id, supplier_type) REFERENCES company (id, type),
    INDEX idx_product_category (category),
    INDEX idx_product_name (name),
    FULLTEXT INDEX ft_product_search (name, description)
);
