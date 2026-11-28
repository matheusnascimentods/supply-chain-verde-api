CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(180) NOT NULL,
    email      VARCHAR(180) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('ADMIN', 'AUDITOR', 'SUPPLIER', 'MANAGER', 'PLATFORM_ADMIN') COLLATE utf8mb4_0900_as_ci NOT NULL,
    company_id BIGINT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT chk_users_company CHECK ((role = 'PLATFORM_ADMIN') = (company_id IS NULL)),
    CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES company (id),
    INDEX idx_users_role (role),
    INDEX idx_users_company_role (company_id, role),
    INDEX idx_users_company_active (company_id, is_active),
    FULLTEXT INDEX ft_users_email (email)
);
