CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE supplier
    ADD COLUMN name_search tsvector
        GENERATED ALWAYS AS (to_tsvector('portuguese'::regconfig, coalesce(name, ''))) STORED,
    ADD COLUMN cnpj_digits TEXT
        GENERATED ALWAYS AS (regexp_replace(cnpj::text, '[^0-9]', '', 'g')) STORED;

CREATE INDEX idx_supplier_name_search
    ON supplier USING GIN (name_search);

CREATE INDEX idx_supplier_cnpj_digits_trgm
    ON supplier USING GIN (cnpj_digits gin_trgm_ops);
