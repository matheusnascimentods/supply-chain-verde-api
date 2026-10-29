---
applyTo: "src/main/resources/db/migration/**/*.sql"
---

# Convenções Flyway e MySQL

- Nunca edite uma migration já aplicada; crie uma nova versão.
- Use nomes no formato `V<numero>__<descricao>.sql`.
- Respeite a ordem das FKs: tabelas referenciadas devem ser criadas antes das dependentes.
- Valores fechados devem permanecer alinhados aos enums do domínio e às colunas `ENUM(...)` do MySQL (collation `utf8mb4_0900_as_ci`).
- `Chain.originAddressId` e `Chain.destinationAddressId` são nullable.
- Seeds de desenvolvimento devem ser determinísticos e não podem conter credenciais de produção.
- Ao alterar schema, atualize entidades JPA, mappers, contratos e testes relacionados.
- Verifique a migration contra um MySQL 8.4 real ou Testcontainers; não valide apenas pela sintaxe.
- Não remova ou reordene migrations existentes. Exceção única: a feature 008 trocou as V1–V39 PostgreSQL pelo baseline MySQL V1–V6; o histórico antigo fica no git.
- DDL no MySQL faz commit implícito: uma migration que falha no meio não é desfeita. Prefira migrations pequenas.
- Datas e horas em `DATETIME(6)`, no horário de São Paulo (fuso do servidor e da JVM). `updated_at` usa `ON UPDATE CURRENT_TIMESTAMP(6)`.
- Tabela de negócio nova precisa de triggers `audit_<tabela>_{insert,update,delete}` com `DEFINER = 'supply_audit'@'%'`, grant `SELECT, TRIGGER` para `supply_audit` e grants por tabela para `${app_user}`. Nunca dê `GRANT ALL` no banco: liberaria `audit_log`.
- Procedures da app usam `SQL SECURITY INVOKER`. Blocos com `;` vão entre `DELIMITER $$` e `DELIMITER ;`.

