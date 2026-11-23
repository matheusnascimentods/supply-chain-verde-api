-- RN-03: ai_ci (accent-insensitive). Alterar a collation reconstrói os índices FULLTEXT das colunas.
ALTER TABLE product
    MODIFY name        VARCHAR(180) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    MODIFY description TEXT         COLLATE utf8mb4_0900_ai_ci NULL;

-- RN-04: índice próprio para o CNPJ, já que o MATCH exige a lista exata de colunas do índice.
ALTER TABLE company
    MODIFY name VARCHAR(180) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    ADD FULLTEXT INDEX ft_company_cnpj (cnpj);
