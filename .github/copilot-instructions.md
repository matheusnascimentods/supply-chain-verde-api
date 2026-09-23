# Instruções do GitHub Copilot — Supply Chain Verde API

## Contexto

Projeto acadêmico da UC Banco de Dados (Anhembi Morumbi): API Java para rastreabilidade de lotes, cálculo de emissões de CO₂, certificações ambientais, relatórios de sustentabilidade e auditoria.

Antes de alterar código, consulte:

- `docs/spec.md` para requisitos funcionais;
- `docs/plan.md` para arquitetura e decisões técnicas;
- `docs/reference.md` para entidades, endpoints, DTOs e configuração;
- `docs/tasks.md` para o checklist de implementação.

## Arquitetura obrigatória

O projeto usa Clean Architecture:

```text
domain <- application <- infrastructure
```

- `domain` não importa Spring, JPA, HTTP ou classes de `application`.
- `application` depende somente de `domain` e orquestra casos de uso.
- `infrastructure` implementa contratos do domínio e integra frameworks, banco, HTTP, segurança e AOP.
- Não mova regra de negócio para controllers, entidades JPA ou mappers.
- Reutilize interfaces de repositório, serviços e exceções existentes antes de criar abstrações novas.

## Regras de implementação

- Use Java 25 e as versões/dependências já declaradas no `pom.xml`.
- DTOs e Value Objects são `record`.
- Entidades de domínio são classes; entidades JPA são classes mutáveis com construtor sem argumentos e Lombok.
- Mappers são interfaces MapStruct com `@Mapper(componentModel = "spring")`; nunca escreva manualmente a implementação gerada.
- Enums do domínio e do schema PostgreSQL não devem ser substituídos por `String`.
- IDs e FKs usam o padrão `entityId`; datas e timestamps usam `xAt`.
- Casos de uso não gravam `AuditLog` manualmente; a auditoria é responsabilidade do `AuditLogInterceptor`.
- Senhas nunca são persistidas em texto puro; use a abstração `PasswordHasher` e BCrypt na infraestrutura.
- Não introduza segredos, defaults inseguros ou arquivos `.env` versionados.
- Erros devem ser explícitos e tratados pelo mecanismo padrão de exceções; não use catches amplos ou fallbacks silenciosos.
- Preserve compatibilidade dos contratos HTTP e atualize a documentação relacionada quando o comportamento mudar.

## Processo de mudança

1. Leia o código e a documentação relevante antes de editar.
2. Faça alterações pequenas e alinhadas aos padrões existentes.
3. Atualize `docs/tasks.md` quando uma etapa do checklist mudar de estado.
4. Execute a menor validação adequada; para alterações amplas use `./mvnw --batch-mode verify`.
5. Não reverta alterações pré-existentes nem altere arquivos fora do escopo.

## Comandos principais

```bash
./mvnw --batch-mode test
./mvnw --batch-mode verify
./mvnw spring-boot:run
docker compose up -d
```

