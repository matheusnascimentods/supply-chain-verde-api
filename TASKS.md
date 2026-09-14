# TASKS.md — Supply Chain Verde API

> Checklist de implementação, ordenado por dependência (de dentro para fora, seguindo a Clean Architecture). Marque `[x]` conforme for concluindo. Referências de arquivo exatas em `estrutura-pastas-backend-java.md`; contratos de DTO em `CLAUDE.md` (seção 8).

**Como usar em dupla:** a partir do fim da Fase 1 (Domain), as Fases 3 (Persistence) e 4 (Application) só dependem do Domain — não uma da outra — então dá pra dividir o trabalho entre os dois integrantes em paralelo a partir daí.

---

## Fase 0 — Bootstrap & Ambiente

*Nenhuma dependência. Faça isso primeiro — sem isso, nada roda.*

- [x] Gerar o projeto no Spring Initializr (Java 25, Maven, Spring Boot 4.1.x, `br.com.anhembi.supplychainverde`)
- [x] Adicionar dependências manuais no `pom.xml`: MapStruct + `mapstruct-processor`, `lombok-mapstruct-binding`, JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- [x] Configurar `maven-compiler-plugin` com ordem correta dos annotation processors (Lombok antes do MapStruct)
- [x] Configurar `application.properties` com placeholders e defaults para desenvolvimento local (sem `.env`, ver seção 10 do `CLAUDE.md`)
- [x] Criar `docker-compose.yml` para orquestrar o PostgreSQL local de desenvolvimento
- [x] Validar que a aplicação compila e sobe vazia (`mvn spring-boot:run`) antes de escrever qualquer entidade
- [x] Configurar `.gitignore` (target/, IDE files)
- [x] Criar repositório no GitHub com a description já definida e conectar o remoto

---

## Fase 1 — Domain Layer

*Zero dependências externas. Pode ser feita inteira antes de qualquer outra fase.*

### Enums (`domain/enums/`)
- [ ] `CertificationStatus`
- [ ] `ProductCategory`
- [ ] `ProductUnit`
- [ ] `StageType`
- [ ] `TransportMode`
- [ ] `FuelType`
- [ ] `CalculationMethod`
- [ ] `UserRole`
- [ ] `AuditAction`

### Value Objects (`domain/valueobject/`) — implementados como `record`
- [ ] `Cnpj` (com validação no construtor compacto)
- [ ] `EmissionFactor`

### Entidades (`domain/entity/`) — na ordem que respeita as referências entre elas
- [ ] `Address`, `User` (sem dependência de outra entidade)
- [ ] `Supplier` (referencia `Address`), `Product`
- [ ] `Certification` (referencia `Supplier`), `Batch` (referencia `Product` + `Supplier`)
- [ ] `Chain` (referencia `Batch`, `Address` ×2, `User`)
- [ ] `Transport`, `CarbonEmission` (referenciam `Chain`)
- [ ] `Report` (referencia `Supplier`)
- [ ] `AuditLog` (referencia `User`)

### Repositórios — apenas interfaces (`domain/repository/`)
- [ ] Uma interface por entidade: `SupplierRepository`, `CertificationRepository`, `ProductRepository`, `BatchRepository`, `AddressRepository`, `ChainRepository`, `TransportRepository`, `CarbonEmissionRepository`, `ReportRepository`, `UserRepository`, `AuditLogRepository`

### Serviços de Domínio (`domain/service/`)
- [ ] `PasswordHasher` (interface — implementação concreta fica na Fase 6)
- [ ] `CarbonFootprintCalculator` (soma emissões de todas as etapas de um lote)
- [ ] `SustainabilityScoreCalculator` (score do fornecedor: certificações + emissões)

### Exceções (`domain/exception/`)
- [ ] `DomainException` (classe base)
- [ ] `SupplierNotFoundException`, `BatchNotFoundException`, `CertificationExpiredException`, `InvalidStageTransitionException`

---

## Fase 2 — Migrations (Flyway)

*Depende de: Fase 1 (schema deve refletir as entidades de domínio). Pode ser feita em paralelo com a Fase 1 por outra pessoa, já que é SQL puro.*

**Decisão pendente antes de começar:** `Chain.originAddressId`/`destinationAddressId` — nullable ou não? (Recomendação registrada: nullable.)

- [ ] `V1__create_address.sql`
- [ ] `V2__create_supplier.sql`
- [ ] `V3__create_certification.sql`
- [ ] `V4__create_product.sql`
- [ ] `V5__create_batch.sql`
- [ ] `V6__create_user.sql`
- [ ] `V7__create_chain.sql`
- [ ] `V8__create_transport.sql`
- [ ] `V9__create_carbon_emission.sql`
- [ ] `V10__create_report.sql`
- [ ] `V11__create_audit_log.sql`
- [ ] Rodar `mvn flyway:migrate` e confirmar todas as tabelas criadas corretamente no Postgres local

---

## Fase 3 — Persistence Layer (JPA)

*Depende de: Fases 1 e 2.*

### Entidades JPA (`infrastructure/persistence/jpa/`) — classes com Lombok, nunca record
- [ ] Uma `*JpaEntity` por entidade (11 no total, mesma ordem da Fase 1)

### Repositórios Spring Data (`infrastructure/persistence/repository/`)
- [ ] Uma `*JpaRepository` (interface) por entidade

### Implementações (`infrastructure/persistence/repositoryimpl/`)
- [ ] Uma `*RepositoryImpl` por entidade, implementando a interface do domain (Fase 1)

### Mappers JPA (`infrastructure/persistence/mapper/`) — interfaces `@Mapper`
- [ ] Uma `*JpaMapper` por entidade (JPA entity ↔ domain entity)

---

## Fase 4 — Application Layer

*Depende apenas da Fase 1 (domain) — pode ser feita em paralelo com a Fase 3.*

### DTOs (`application/dto/`) — `record`, ver contratos completos no `CLAUDE.md` (seção 8)
- [ ] Address, Supplier (+ `SupplierRankingDTO`), Certification, Product, Batch, Chain, Transport, CarbonEmission (+ `CarbonFootprintResponseDTO`), Report, User, Auth

### Mappers de Aplicação (`application/mapper/`) — interfaces `@Mapper`
- [ ] Uma `*DtoMapper` por entidade (DTO ↔ domain entity)

### Casos de Uso (`application/usecase/`) — ver descrições completas no `CLAUDE.md` (seção 6)
- [ ] `supplier/`: Register, Update, Get, List, RankBySustainability
- [ ] `certification/`: Register, UpdateStatus, ListExpiring
- [ ] `product/`: Register, Update, List
- [ ] `batch/`: Register, GetTraceability, ListBySupplier
- [ ] `chain/`: RegisterStage, ListStagesByBatch
- [ ] `transport/`: Register
- [ ] `emission/`: Calculate, GetBatchCarbonFootprint
- [ ] `report/`: Generate, Get, ListBySupplier
- [ ] `user/`: Register, Authenticate, UpdateRole
- [ ] `audit/`: ListAuditLogs

### Exceções de Aplicação (`application/exception/`)
- [ ] `ApplicationException` (base), `ValidationException`, `UnauthorizedActionException`

---

## Fase 5 — Web Layer

*Depende de: Fases 3 e 4 completas.*

- [ ] Controllers (`infrastructure/web/controller/`) — um por recurso, seguindo as rotas exatas do `CLAUDE.md` (seção 7): Supplier, Certification, Product, Batch, Chain, Transport, CarbonEmission, Report, User, Auth, AuditLog
- [ ] `GlobalExceptionHandler` (`infrastructure/web/advice/`) — traduzir exceções de domain/application em status HTTP
- [ ] `SwaggerConfig`, `CorsConfig`, `WebConfig` (`infrastructure/web/config/`)

---

## Fase 6 — Security

*Depende de: `User` (Fases 1 e 3) e `PasswordHasher` (interface da Fase 1).*

- [ ] `BcryptPasswordHasher` (implementa `PasswordHasher` do domain)
- [ ] `JwtTokenProvider` (emissão e validação do token)
- [ ] `JwtAuthenticationFilter`
- [ ] `CustomUserDetailsService`
- [ ] `SecurityConfig` (regras de autorização por `role`, rotas públicas: login + rastreabilidade/pegada de carbono via QR Code)
- [ ] Testar o fluxo completo: login → token → chamada autenticada → chamada sem token em rota protegida (deve retornar 401/403)

---

## Fase 7 — Auditoria

*Depende de: `AuditLog` (Fases 1 e 3).*

- [ ] `AuditLogInterceptor` (AOP, `infrastructure/audit/`) — nenhum caso de uso deve chamar isso manualmente
- [ ] Validar que uma ação de escrita (ex: criar `Supplier`) gera um registro em `AuditLog` automaticamente

---

## Fase 8 — Testes

*Interligada com as fases anteriores — idealmente feita junto de cada fase, não só no fim.*

- [ ] Testes unitários (`*Test.java`) — `domain/`: `CarbonFootprintCalculator`, `SustainabilityScoreCalculator`, `Cnpj`
- [ ] Testes unitários (`*Test.java`) — `application/`: cada Use Case, com mocks de repository
- [ ] Testes de integração (`*IT.java`) — `infrastructure/`: controllers + repositories, com Testcontainers + Postgres real
- [ ] Dados de teste (seed) para consultas de relatório exigidas no edital

---

## Fase 9 — Entregáveis do Edital (não-código)

*Podem ser feitas em paralelo com qualquer fase acima.*

- [ ] Levantamento de Requisitos (entregável 1)
- [ ] Plano de Segurança e Administração de Dados formal (entregável 5) — usar a Fase 6 como referência técnica
- [ ] Proposta de Arquitetura de Dados — diagrama formal (entregável 3)
- [ ] Cronograma consolidado (documentar em `README.md`)
- [ ] Trilha Oracle Academy (documentar em `README.md`)