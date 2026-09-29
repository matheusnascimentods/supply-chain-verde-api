ALTER TABLE product
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            to_tsvector(
                'portuguese'::regconfig,
                coalesce(name, '') || ' ' || coalesce(description, '')
            )
        ) STORED;

CREATE INDEX idx_product_search_vector
    ON product USING GIN (search_vector);
