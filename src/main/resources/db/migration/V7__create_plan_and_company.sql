-- Feature 010: catálogo de planos, companhias (mercado ou fornecedor) e assinaturas.
-- uq_company_id_type é o alvo das FKs compostas que restringem o tipo da companhia (mesmo truque de
-- chain.previous_stage_type na V1): a coluna gerada constante do lado de quem referencia fixa o tipo.

CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       ENUM('BASIC', 'PRO', 'PRO_MAX') COLLATE utf8mb4_0900_as_ci NOT NULL,
    name       VARCHAR(60) NOT NULL,
    max_users  INT         NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_plan_code UNIQUE (code),
    CONSTRAINT chk_plan_max_users_positive CHECK (max_users > 0)
);

INSERT INTO plan (code, name, max_users) VALUES
    ('BASIC', 'Basic', 3),
    ('PRO', 'Pro', 10),
    ('PRO_MAX', 'Pro Max', 15);

CREATE TABLE company (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(180) COLLATE utf8mb4_0900_as_ci NOT NULL,
    cnpj       CHAR(14)     NOT NULL,
    address_id BIGINT       NOT NULL,
    phone      VARCHAR(30)  NULL,
    email      VARCHAR(180) NOT NULL,
    type       ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_company_cnpj UNIQUE (cnpj),
    CONSTRAINT uq_company_id_type UNIQUE (id, type),
    CONSTRAINT fk_company_address FOREIGN KEY (address_id) REFERENCES address (id),
    INDEX idx_company_address_id (address_id),
    INDEX idx_company_name (name),
    INDEX idx_company_type (type),
    FULLTEXT INDEX ft_company_name (name)
);

-- RN-03: active_company_id só tem valor na assinatura ativa, e o UNIQUE ignora NULL, então cada
-- companhia tem no máximo uma assinatura com ended_at nulo.
CREATE TABLE company_plan (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id        BIGINT      NOT NULL,
    plan_id           BIGINT      NOT NULL,
    started_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at          DATETIME(6) NULL,
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    active_company_id BIGINT GENERATED ALWAYS AS (IF(ended_at IS NULL, company_id, NULL)) STORED,
    CONSTRAINT uq_company_plan_active UNIQUE (active_company_id),
    CONSTRAINT chk_company_plan_period CHECK (ended_at IS NULL OR ended_at >= started_at),
    CONSTRAINT fk_company_plan_company FOREIGN KEY (company_id) REFERENCES company (id),
    CONSTRAINT fk_company_plan_plan FOREIGN KEY (plan_id) REFERENCES plan (id),
    INDEX idx_company_plan_company_id (company_id),
    INDEX idx_company_plan_plan_id (plan_id)
);
