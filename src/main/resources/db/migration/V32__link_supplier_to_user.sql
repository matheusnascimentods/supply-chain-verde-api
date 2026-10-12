ALTER TABLE supplier ADD COLUMN user_id BIGINT NULL;

ALTER TABLE supplier
    ADD CONSTRAINT uq_supplier_user UNIQUE (user_id),
    ADD CONSTRAINT fk_supplier_user FOREIGN KEY (user_id) REFERENCES users (user_id);

-- Migra a convenção atual (userId == supplierId).
UPDATE supplier s
SET user_id = u.user_id
    FROM users u
WHERE u.role = 'SUPPLIER' AND u.user_id = s.supplier_id;