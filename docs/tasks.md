# tasks.md — Supply Chain Verde API

> Checklist derivado do `plan.md`, ordenado pelas dependências da Clean Architecture. Marque `[x]` conforme a implementação evoluir.

## Fase 0 — Bootstrap e ambiente

- [x] Criar projeto Spring Boot com Java 25 e Maven.
- [x] Configurar dependências de Web, JPA, Security, Flyway, Validation, AOP e Actuator.
- [x] Configurar MapStruct, Lombok e `lombok-mapstruct-binding`.
- [x] Configurar JJWT, JaCoCo, Docker Compose e propriedades por ambiente.
- [x] Configurar CI, CodeQL, Dependency Review e templates do GitHub.

## Fase 1 — Domínio

### Enums e value objects

- [x] Criar enums de certificação, produto, cadeia, transporte, emissão, usuário e auditoria.
- [x] Criar `Cnpj` com validação.
- [x] Criar `EmissionFactor`.

### Entidades e contratos

- [x] Criar as 11 entidades de domínio.
- [x] Criar uma interface de repositório por entidade.
- [x] Criar `CarbonFootprintCalculator`, `SustainabilityScoreCalculator` e `PasswordHasher`.
- [x] Criar exceções de domínio.

## Fase 2 — Schema e migrations

- [x] Criar migrations das 11 tabelas na ordem das FKs.
- [x] Tornar endereços de origem/destino da etapa opcionais.
- [x] Criar seeds de desenvolvimento e demonstração.
- [x] Validar execução das migrations contra PostgreSQL.

## Fase 3 — Persistência JPA

- [x] Criar as 11 entidades JPA.
- [x] Criar repositórios Spring Data.
- [x] Criar implementações dos repositórios de domínio.
- [x] Criar mappers JPA com MapStruct.
- [x] Validar conversões entre JPA e domínio.

## Fase 4 — Aplicação

- [x] Criar DTOs de request/response como records.
- [x] Criar mappers de aplicação.
- [x] Implementar casos de uso de fornecedores e certificações.
- [x] Implementar casos de uso de produtos e lotes.
- [x] Implementar casos de uso de etapas, transporte e emissões.
- [x] Implementar casos de uso de relatórios.
- [x] Implementar autenticação, usuários e auditoria.
- [x] Criar exceções de aplicação.

## Fase 5 — Web API

- [x] Criar controllers para todos os recursos.
- [ ] Implementar e documentar `GET /api/v1/certifications` para listar certificações.
- [x] Criar tratamento global de exceções.
- [x] Configurar CORS, Web MVC e OpenAPI.
- [x] Conferir rotas, DTOs e códigos HTTP contra `spec.md`.

## Fase 6 — Segurança

- [x] Implementar hash BCrypt.
- [x] Implementar emissão e validação de JWT.
- [x] Implementar filtro de autenticação.
- [x] Implementar carregamento de usuário.
- [x] Configurar autorização por role.
- [x] Manter login e rastreabilidade/pegada públicas.
- [x] Testar login, token válido, token ausente e acesso proibido.

## Fase 7 — Auditoria

- [x] Implementar `AuditLogInterceptor`.
- [x] Garantir que casos de uso não gravem auditoria manualmente.
- [x] Validar geração automática de log em operações de escrita.
- [x] Expor consulta autorizada dos logs.

## Fase 8 — Testes e qualidade

- [x] Cobrir value objects e serviços de domínio.
- [x] Cobrir casos de uso com mocks.
- [x] Cobrir controllers e persistência com Testcontainers.
- [x] Cobrir seeds e consultas de relatório.
- [x] Executar `./mvnw --batch-mode verify`.
- [x] Publicar cobertura JaCoCo no pipeline.

## Fase 10 — Dashboard

- [ ] Criar e documentar a rota `GET /api/v1/dashboard/summary` com os indicadores e lotes recentes necessários à tela de dashboard.

## Fase 9 — Entregáveis e evolução

- [ ] Formalizar levantamento de requisitos.
- [ ] Formalizar plano de segurança e administração de dados.
- [ ] Criar proposta de arquitetura de dados.
- [ ] Consolidar cronograma no README.
- [ ] Documentar trilha Oracle Academy.
- [ ] Decidir refresh token.
- [ ] Decidir mitigação de enumeração de IDs públicos.
- [ ] Avaliar rate limiting dos endpoints públicos.
