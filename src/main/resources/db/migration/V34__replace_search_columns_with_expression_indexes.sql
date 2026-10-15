ALTER TABLE supplier DROP COLUMN name_search, DROP COLUMN cnpj_digits;
ALTER TABLE product DROP COLUMN search_vector;

CREATE INDEX idx_supplier_name_fts
    ON supplier USING GIN (to_tsvector('portuguese'::regconfig, coalesce(name, '')));

CREATE INDEX idx_supplier_cnpj_digits_trgm
    ON supplier USING GIN (regexp_replace(cnpj::text, '[^0-9]', '', 'g') gin_trgm_ops);

CREATE INDEX idx_product_fts
    ON product USING GIN (
        to_tsvector('portuguese'::regconfig, coalesce(name, '') || ' ' || coalesce(description, ''))
    );
