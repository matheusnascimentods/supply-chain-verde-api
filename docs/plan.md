# plan.md — Supply Chain Verde API

> Tradução técnica do `spec.md`: como construir e manter o backend. A implementação segue Clean Architecture, DDD e separação explícita entre domínio, aplicação e infraestrutura.

## 1. Stack

| Item | Escolha |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.0.x |
| Persistência | Hibernate/JPA e Spring Data |
| Banco | PostgreSQL 17 |
| Migrations | Flyway, SQL versionado |
| Mapeamento | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Segurança | Spring Security, JJWT 0.12.6 e BCrypt |
| API | Spring Web MVC, Bean Validation e OpenAPI/Swagger |
| Testes | JUnit 5, Mockito e Testcontainers |
| Qualidade | Maven, JaCoCo, CodeQL e Dependency Review |

## 2. Arquitetura

```text
domain          <- regras de negócio puras, entidades, VOs e contratos
   ^
application     <- casos de uso, DTOs e mappers de aplicação
   ^
infrastructure  <- Spring, HTTP, JPA, segurança, AOP e configurações
```

O domínio não importa Spring, JPA ou classes da aplicação. A aplicação depende apenas do domínio. A infraestrutura implementa os contratos internos e adapta o mundo externo ao modelo de negócio.

## 3. Estrutura de pastas

```text
src/main/java/br/com/anhembi/supplychainverde/
├── domain/
│   ├── entity/
│   ├── enums/
│   ├── valueobject/
│   ├── repository/
│   ├── service/
│   └── exception/
├── application/
│   ├── dto/
│   ├── mapper/
│   ├── usecase/
│   └── exception/
├── infrastructure/
│   ├── web/
│   ├── persistence/
│   ├── security/
│   ├── audit/
│   └── config/
└── SupplyChainVerdeApplication.java

src/main/resources/
├── db/migration/
├── application.properties
└── application-test.properties
```

## 4. Camada de domínio

- Entidades de negócio são classes mutáveis com invariantes próprias.
- Value Objects são records validados, como `Cnpj` e `EmissionFactor`.
- Enums representam todos os conjuntos fechados do modelo.
- Interfaces de repositório ficam no domínio.
- Serviços puros concentram cálculo de pegada, score e abstração de hash.
- Exceções de domínio representam falhas sem conhecimento de HTTP.

Entidades principais: `Address`, `User`, `Supplier`, `Certification`, `Product`, `Batch`, `Chain`, `Transport`, `CarbonEmission`, `Report` e `AuditLog`.

## 5. Camada de aplicação

Cada caso de uso tem responsabilidade única e orquestra portas do domínio:

- `supplier`: cadastro, consulta, atualização e ranking.
- `certification`: cadastro, status e vencimentos.
- `product`: cadastro, atualização e listagem.
- `batch`: cadastro, rastreabilidade e listagem por fornecedor.
- `chain`: registro e consulta de etapas.
- `transport`: registro de transporte.
- `emission`: cálculo e consolidação de carbono.
- `report`: geração e consulta de relatórios.
- `user`: cadastro, autenticação e alteração de perfil.
- `audit`: consulta da trilha de auditoria.

DTOs são records imutáveis. Mappers DTO/domínio são interfaces MapStruct geradas em compilação.

## 6. Persistência

- Cada entidade de domínio possui uma `*JpaEntity`.
- Cada entidade possui um repositório Spring Data e uma implementação do contrato de domínio.
- Mappers JPA isolam o modelo relacional do modelo de negócio.
- Flyway cria o schema em ordem de dependência, de `V1__create_address.sql` a `V11__create_audit_log.sql`.
- Seeds de desenvolvimento ficam nas migrations `V12` a `V22`.
- `originAddressId` e `destinationAddressId` de `Chain` são nullable.

## 7. Web, segurança e auditoria

- Controllers REST ficam em `infrastructure/web/controller`.
- `GlobalExceptionHandler` converte exceções de domínio/aplicação em respostas HTTP consistentes.
- OpenAPI documenta os endpoints em `/swagger-ui.html` e `/v3/api-docs`.
- `JwtAuthenticationFilter` valida o bearer token e popula o contexto do Spring Security.
- `SecurityConfig` aplica as regras RBAC e mantém login/rastreabilidade públicos.
- `BcryptPasswordHasher` implementa a porta de hash do domínio.
- `AuditLogInterceptor` registra ações relevantes automaticamente via AOP.

## 8. Convenções

- IDs e FKs seguem `entityId`, como `supplierId` e `batchId`.
- Datas usam `xAt`, como `createdAt` e `periodStartAt`.
- DTOs usam `record`; entidades JPA usam classes com construtor sem argumentos.
- Mappers são interfaces `@Mapper(componentModel = "spring")`.
- Nenhum segredo deve ser commitado; configuração usa variáveis de ambiente.
- Falhas devem ser explícitas e passar pelo mecanismo padrão de exceções/logs.

## 9. Configuração

`application.properties` usa placeholders para porta, datasource, Flyway, CORS e JWT. Em ambientes reais, `SPRING_DATASOURCE_PASSWORD` e `JWT_SECRET` devem ser fornecidos externamente e nunca ter valores previsíveis versionados.

## 10. Estratégia de testes

- Unitários: value objects, serviços de domínio e casos de uso com mocks.
- Integração: controllers, JPA e migrations contra PostgreSQL via Testcontainers.
- `mvn test` cobre testes unitários; `mvn verify` também gera cobertura JaCoCo e executa a validação completa.
- Fluxos críticos: login, autorização, cadastro de lote, etapa/transporte/emissão, rastreabilidade pública e auditoria automática.

## 11. Decisões em aberto

- Refresh token.
- Rate limiting para endpoints públicos.
- UUID público para evitar enumeração de `batchId`.
- Plano formal de segurança, backup e recuperação de desastre.
- Entregáveis acadêmicos de arquitetura de dados e cronograma.

