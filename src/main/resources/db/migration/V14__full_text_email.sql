-- RN-03/RN-05. O UNIQUE passa a ignorar maiúscula/acento: falha se já houver emails que só diferem nisso (desejado).
ALTER TABLE users
    MODIFY email VARCHAR(180) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    ADD FULLTEXT INDEX ft_users_email (email);
