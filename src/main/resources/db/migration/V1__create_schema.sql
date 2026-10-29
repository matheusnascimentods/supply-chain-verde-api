-- Baseline MySQL: estado final das 39 migrations PostgreSQL, que ficam só no histórico do git.
-- Collation padrão do banco é utf8mb4_0900_as_cs (docker-compose). Colunas de busca textual e ENUMs usam
-- utf8mb4_0900_as_ci para MATCH e comparação de categoria ignorarem maiúsculas.
-- updated_at é nativo: ON UPDATE só muda a coluna quando a linha muda de fato.

CREATE TABLE address (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    street       VARCHAR(255) NOT NULL,
    number       VARCHAR(30)  NOT NULL,
    neighborhood VARCHAR(120) NOT NULL,
    complement   VARCHAR(120) NULL,
    zip_code     VARCHAR(20)  NOT NULL,
    city         VARCHAR(120) NOT NULL,
    state        VARCHAR(2)   NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_address_city_state (city, state)
);

CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(180) NOT NULL,
    email      VARCHAR(180) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('ADMIN', 'AUDITOR', 'SUPPLIER', 'MANAGER') COLLATE utf8mb4_0900_as_ci NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT users_email_key UNIQUE (email),
    INDEX idx_users_role (role)
);

CREATE TABLE product (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(180) COLLATE utf8mb4_0900_as_ci NOT NULL,
    category    ENUM('AGRICULTURE', 'LIVESTOCK', 'PROCESSED_FOOD', 'TEXTILE', 'FORESTRY', 'OTHER')
                COLLATE utf8mb4_0900_as_ci NOT NULL,
    unit        ENUM('KG', 'TON', 'LITER', 'UNIT', 'M3') COLLATE utf8mb4_0900_as_ci NOT NULL,
    description TEXT COLLATE utf8mb4_0900_as_ci NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_product_category (category),
    INDEX idx_product_name (name),
    FULLTEXT INDEX ft_product_search (name, description)
);

CREATE TABLE supplier (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(180) COLLATE utf8mb4_0900_as_ci NOT NULL,
    cnpj       CHAR(14)     NOT NULL,
    address_id BIGINT       NOT NULL,
    phone      VARCHAR(30)  NULL,
    user_id    BIGINT       NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT supplier_cnpj_key UNIQUE (cnpj),
    CONSTRAINT uq_supplier_user UNIQUE (user_id),
    CONSTRAINT fk_supplier_address FOREIGN KEY (address_id) REFERENCES address (id),
    CONSTRAINT fk_supplier_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_supplier_address_id (address_id),
    INDEX idx_supplier_name (name),
    FULLTEXT INDEX ft_supplier_name (name)
);

CREATE TABLE certification (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id   BIGINT       NOT NULL,
    certification VARCHAR(180) NOT NULL,
    issuing_body  VARCHAR(180) NOT NULL,
    issued_at     DATE         NOT NULL,
    expires_at    DATE         NOT NULL,
    status        ENUM('ACTIVE', 'EXPIRED', 'SUSPENDED', 'UNDER_REVIEW') COLLATE utf8mb4_0900_as_ci NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_certification_dates CHECK (expires_at >= issued_at),
    CONSTRAINT fk_certification_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    INDEX idx_certification_expires_at (expires_at),
    INDEX idx_certification_status (status),
    INDEX idx_certification_supplier_id (supplier_id)
);

CREATE TABLE batch (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT        NOT NULL,
    supplier_id BIGINT        NOT NULL,
    quantity    DECIMAL(19,4) NOT NULL,
    produced_at DATE          NOT NULL,
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_batch_quantity_positive CHECK (quantity > 0),
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_batch_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    INDEX idx_batch_produced_at (produced_at),
    INDEX idx_batch_product_id (product_id),
    INDEX idx_batch_supplier_id (supplier_id)
);

-- Ordem obrigatória das etapas: cada etapa aponta para a anterior do mesmo lote. PRODUCTION tem
-- previous_stage_type NULL, e o InnoDB não verifica FK com NULL (mesmo MATCH SIMPLE do PostgreSQL).
CREATE TABLE chain (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id               BIGINT      NOT NULL,
    origin_address_id      BIGINT      NULL,
    destination_address_id BIGINT      NULL,
    responsible_user_id    BIGINT      NOT NULL,
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
    CONSTRAINT fk_chain_origin_address FOREIGN KEY (origin_address_id) REFERENCES address (id),
    CONSTRAINT fk_chain_destination_address FOREIGN KEY (destination_address_id) REFERENCES address (id),
    CONSTRAINT fk_chain_responsible_user FOREIGN KEY (responsible_user_id) REFERENCES users (id),
    CONSTRAINT fk_chain_previous_stage FOREIGN KEY (batch_id, previous_stage_type)
        REFERENCES chain (batch_id, stage_type),
    INDEX idx_chain_batch_id (batch_id),
    INDEX idx_chain_destination_address_id (destination_address_id),
    INDEX idx_chain_origin_address_id (origin_address_id),
    INDEX idx_chain_responsible_user_id (responsible_user_id),
    INDEX idx_chain_started_at (started_at)
);

CREATE TABLE transport (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain_id       BIGINT        NOT NULL,
    transport_mode ENUM('ROAD', 'RAIL', 'MARITIME', 'AIR') COLLATE utf8mb4_0900_as_ci NOT NULL,
    distance       DECIMAL(19,4) NOT NULL,
    fuel_type      ENUM('DIESEL', 'BIODIESEL', 'ELECTRIC', 'GASOLINE', 'ETHANOL', 'AVIATION_KEROSENE', 'HEAVY_FUEL_OIL')
                   COLLATE utf8mb4_0900_as_ci NOT NULL,
    capacity       DECIMAL(19,4) NOT NULL,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT transport_chain_id_key UNIQUE (chain_id),
    CONSTRAINT chk_transport_capacity_positive CHECK (capacity > 0),
    CONSTRAINT chk_transport_distance_positive CHECK (distance > 0),
    CONSTRAINT fk_transport_chain FOREIGN KEY (chain_id) REFERENCES chain (id),
    INDEX idx_transport_fuel_type (fuel_type),
    INDEX idx_transport_mode (transport_mode)
);

CREATE TABLE carbon_emission (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain_id           BIGINT        NOT NULL,
    emission_factor    DECIMAL(19,8) NOT NULL,
    co2_kg             DECIMAL(19,4) NOT NULL,
    calculation_method ENUM('DEFRA', 'GHG_PROTOCOL', 'IPCC', 'EMEP_EEA') COLLATE utf8mb4_0900_as_ci NOT NULL,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT carbon_emission_chain_id_key UNIQUE (chain_id),
    CONSTRAINT chk_carbon_emission_co2_non_negative CHECK (co2_kg >= 0),
    CONSTRAINT chk_carbon_emission_factor_non_negative CHECK (emission_factor >= 0),
    CONSTRAINT fk_carbon_emission_chain FOREIGN KEY (chain_id) REFERENCES chain (id),
    INDEX idx_carbon_emission_calculated_at (created_at),
    INDEX idx_carbon_emission_calculation_method (calculation_method)
);

CREATE TABLE report (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id           BIGINT        NOT NULL,
    period_start_at       DATE          NOT NULL,
    period_end_at         DATE          NOT NULL,
    total_co2_kg          DECIMAL(19,4) NOT NULL,
    tracked_product_count INT           NOT NULL,
    batch_count           INT           NOT NULL DEFAULT 0,
    created_at            DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_report_batch_count_non_negative CHECK (batch_count >= 0),
    CONSTRAINT chk_report_period CHECK (period_end_at >= period_start_at),
    CONSTRAINT chk_report_total_co2_non_negative CHECK (total_co2_kg >= 0),
    CONSTRAINT chk_report_tracked_product_count_non_negative CHECK (tracked_product_count >= 0),
    CONSTRAINT fk_report_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    INDEX idx_report_period (period_start_at, period_end_at),
    INDEX idx_report_supplier_id (supplier_id)
);
