---
applyTo: "src/main/resources/db/migration/**/*.sql"
---

# Convenções Flyway e PostgreSQL

- Nunca edite uma migration já aplicada; crie uma nova versão.
- Use nomes no formato `V<numero>__<descricao>.sql`.
- Respeite a ordem das FKs: tabelas referenciadas devem ser criadas antes das dependentes.
- Valores fechados devem permanecer alinhados aos enums do domínio e aos tipos do PostgreSQL.
- `Chain.originAddressId` e `Chain.destinationAddressId` são nullable.
- Seeds de desenvolvimento devem ser determinísticos e não podem conter credenciais de produção.
- Ao alterar schema, atualize entidades JPA, mappers, contratos e testes relacionados.
- Verifique a migration contra um PostgreSQL real ou Testcontainers; não valide apenas pela sintaxe.
- Não remova ou reordene migrations existentes.

