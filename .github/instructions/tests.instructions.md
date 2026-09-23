---
applyTo: "src/test/**/*.java"
---

# Convenções de testes

- Testes unitários usam o sufixo `Test.java`; testes de integração usam `IT.java`.
- Teste value objects e serviços de domínio sem Spring quando possível.
- Teste casos de uso com mocks das interfaces de repositório e serviços.
- Testes de infraestrutura devem validar controllers, persistência, segurança, migrations e integração com PostgreSQL usando Testcontainers quando necessário.
- Cubra fluxos de sucesso, validação, entidades inexistentes, autorização e efeitos colaterais relevantes.
- Em testes de auditoria, confirme que a ação gera o log automaticamente e que nenhum caso de uso precisa invocá-lo diretamente.
- Evite testes frágeis dependentes da ordem global ou de dados externos.
- Execute `./mvnw --batch-mode test` para testes unitários e `./mvnw --batch-mode verify` para a validação completa e JaCoCo.

