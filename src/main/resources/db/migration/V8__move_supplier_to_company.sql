-- Feature 010: move os dados de supplier para company (RN-24 a RN-28). A tabela supplier só sai na V9.
-- As triggers de auditoria das tabelas alteradas aqui saem antes e voltam na V9 com as colunas novas:
-- assim a migração não gera eventos, como o seed (RN-07 da V5).
DROP TRIGGER audit_users_insert;
DROP TRIGGER audit_users_update;
DROP TRIGGER audit_users_delete;
DROP TRIGGER audit_product_insert;
DROP TRIGGER audit_product_update;
DROP TRIGGER audit_product_delete;
DROP TRIGGER audit_batch_insert;
DROP TRIGGER audit_batch_update;
DROP TRIGGER audit_batch_delete;
DROP TRIGGER audit_report_insert;
DROP TRIGGER audit_report_update;
DROP TRIGGER audit_report_delete;

-- 1. Fornecedores viram companhias SUPPLIER com o mesmo id, então batch, certification e report não mudam.
INSERT INTO company (id, name, cnpj, address_id, phone, email, type, created_at, updated_at)
SELECT s.id, s.name, s.cnpj, s.address_id, s.phone,
       COALESCE(u.email, CONCAT('fornecedor', s.id, '@supplychainverde.local')),
       'SUPPLIER', s.created_at, s.updated_at
FROM supplier s
LEFT JOIN users u ON u.id = s.user_id;

-- 2. Mercado de demonstração (RN-25), com id logo depois do maior fornecedor. A trigger de address
-- continua ativa, então o endereço gera um INSERT sem ator na auditoria.
INSERT INTO address (street, number, neighborhood, complement, zip_code, city, state)
VALUES ('Avenida Paulista', '1000', 'Bela Vista', 'Sede do mercado de demonstração', '01310-100', 'São Paulo', 'SP');

SET @demo_market_id = (SELECT COALESCE(MAX(id), 0) + 1 FROM company);

INSERT INTO company (id, name, cnpj, address_id, phone, email, type)
VALUES (@demo_market_id, 'Mercado Verde Demonstração', '11444777000161', LAST_INSERT_ID(), '(11) 3000-0000',
        'contato@mercadoverde.local', 'MARKET');

INSERT INTO company_plan (company_id, plan_id)
SELECT c.id, p.id
FROM company c
JOIN plan p ON p.code = IF(c.type = 'MARKET', 'PRO_MAX', 'BASIC');

-- 3. Usuários.
ALTER TABLE users
    MODIFY role ENUM('ADMIN', 'AUDITOR', 'SUPPLIER', 'MANAGER', 'PLATFORM_ADMIN') COLLATE utf8mb4_0900_as_ci NOT NULL,
    ADD COLUMN company_id BIGINT NULL AFTER role,
    ADD INDEX idx_users_company_role (company_id, role),
    ADD CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES company (id);

-- RN-24: o usuário vinculado vira o ADMIN do fornecedor.
UPDATE users u
JOIN supplier s ON s.user_id = u.id
SET u.company_id = s.id, u.role = 'ADMIN';

-- RN-26: o ADMIN de menor id fora dos fornecedores vira PLATFORM_ADMIN.
SET @platform_admin_id = (SELECT MIN(id) FROM users WHERE role = 'ADMIN' AND company_id IS NULL);
UPDATE users SET role = 'PLATFORM_ADMIN' WHERE id = @platform_admin_id;

-- Usuário SUPPLIER sem fornecedor não tem companhia de origem nem perfil válido no mercado (RN-07):
-- entra no mercado de demonstração como AUDITOR, que só consulta.
UPDATE users SET role = 'AUDITOR' WHERE role = 'SUPPLIER' AND company_id IS NULL;

-- RN-25: o resto vai para o mercado de demonstração.
UPDATE users SET company_id = @demo_market_id WHERE company_id IS NULL AND role <> 'PLATFORM_ADMIN';

ALTER TABLE users
    ADD CONSTRAINT chk_users_company CHECK ((role = 'PLATFORM_ADMIN') = (company_id IS NULL));

-- 4. Produtos (RN-28): o fornecedor de menor id entre os lotes fica com a linha original, os outros
-- recebem cópias. Produto sem lote vai para o fornecedor de menor id.
ALTER TABLE product
    ADD COLUMN supplier_id BIGINT NULL AFTER id,
    ADD COLUMN source_product_id BIGINT NULL;

CREATE TEMPORARY TABLE tmp_product_supplier AS
SELECT DISTINCT product_id, supplier_id FROM batch;

UPDATE product p
LEFT JOIN (SELECT product_id, MIN(supplier_id) AS supplier_id
           FROM tmp_product_supplier
           GROUP BY product_id) owner ON owner.product_id = p.id
SET p.supplier_id = COALESCE(owner.supplier_id, (SELECT MIN(id) FROM company WHERE type = 'SUPPLIER'));

INSERT INTO product (supplier_id, name, category, unit, description, source_product_id, created_at, updated_at)
SELECT t.supplier_id, p.name, p.category, p.unit, p.description, p.id, p.created_at, p.updated_at
FROM tmp_product_supplier t
JOIN product p ON p.id = t.product_id
WHERE t.supplier_id <> p.supplier_id;

UPDATE batch b
JOIN product dup ON dup.source_product_id = b.product_id AND dup.supplier_id = b.supplier_id
SET b.product_id = dup.id;

DROP TEMPORARY TABLE tmp_product_supplier;

ALTER TABLE product
    DROP COLUMN source_product_id,
    MODIFY supplier_id BIGINT NOT NULL,
    ADD COLUMN supplier_type ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
        GENERATED ALWAYS AS ('SUPPLIER') STORED,
    ADD CONSTRAINT uq_product_id_supplier UNIQUE (id, supplier_id),
    ADD CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id, supplier_type) REFERENCES company (id, type);

-- 5. Lotes: todos do mercado de demonstração e ACTIVE (RN-27). O ADD COLUMN preenche status com o primeiro
-- valor do ENUM; o UPDATE corrige.
ALTER TABLE batch
    ADD COLUMN market_id BIGINT NULL AFTER supplier_id,
    ADD COLUMN status ENUM('PENDING', 'ACTIVE', 'REJECTED') COLLATE utf8mb4_0900_as_ci NOT NULL AFTER market_id,
    ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER status,
    MODIFY produced_at DATE NULL;

UPDATE batch SET market_id = @demo_market_id, status = 'ACTIVE';

ALTER TABLE batch DROP FOREIGN KEY fk_batch_supplier;

ALTER TABLE batch
    MODIFY market_id BIGINT NOT NULL,
    ADD COLUMN market_type ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
        GENERATED ALWAYS AS ('MARKET') STORED,
    ADD COLUMN supplier_type ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
        GENERATED ALWAYS AS ('SUPPLIER') STORED,
    ADD CONSTRAINT chk_batch_rejection_reason CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL)),
    ADD CONSTRAINT chk_batch_active_produced_at CHECK (status <> 'ACTIVE' OR produced_at IS NOT NULL),
    ADD CONSTRAINT fk_batch_market FOREIGN KEY (market_id, market_type) REFERENCES company (id, type),
    ADD CONSTRAINT fk_batch_supplier FOREIGN KEY (supplier_id, supplier_type) REFERENCES company (id, type),
    ADD CONSTRAINT fk_batch_product_supplier FOREIGN KEY (product_id, supplier_id) REFERENCES product (id, supplier_id),
    ADD INDEX idx_batch_market_id (market_id),
    ADD INDEX idx_batch_status (status);

-- 6. Relatórios: todos do mercado de demonstração.
ALTER TABLE report ADD COLUMN market_id BIGINT NULL AFTER supplier_id;

UPDATE report SET market_id = @demo_market_id;

ALTER TABLE report DROP FOREIGN KEY fk_report_supplier;

ALTER TABLE report
    MODIFY market_id BIGINT NOT NULL,
    ADD COLUMN market_type ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
        GENERATED ALWAYS AS ('MARKET') STORED,
    ADD COLUMN supplier_type ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
        GENERATED ALWAYS AS ('SUPPLIER') STORED,
    ADD CONSTRAINT fk_report_market FOREIGN KEY (market_id, market_type) REFERENCES company (id, type),
    ADD CONSTRAINT fk_report_supplier FOREIGN KEY (supplier_id, supplier_type) REFERENCES company (id, type),
    ADD INDEX idx_report_market_id (market_id);

-- 7. Certificações (RN-02).
ALTER TABLE certification DROP FOREIGN KEY fk_certification_supplier;

ALTER TABLE certification
    ADD COLUMN supplier_type ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci
        GENERATED ALWAYS AS ('SUPPLIER') STORED,
    ADD CONSTRAINT fk_certification_supplier FOREIGN KEY (supplier_id, supplier_type) REFERENCES company (id, type);
