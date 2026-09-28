# tasks.md — Supply Chain Verde API

> Checklist derivado do `plan.md`, ordenado pelas dependências da Clean Architecture. Marque `[x]` conforme a implementação evoluir.

## Fase 0 — Bootstrap e ambiente

- [x] Criar projeto Spring Boot com Java 25 e Maven.
  - Estrutura Maven executável por `./mvnw`; aplicação iniciada por `SupplyChainVerdeApplication`.
- [x] Configurar dependências de Web, JPA, Security, Flyway, Validation, AOP e Actuator.
  - Web expõe a API REST; JPA persiste no PostgreSQL; Flyway versiona o schema; Security aplica autenticação/autorização; Validation verifica entradas; AOP oferece interceptação transversal; Actuator expõe health.
- [x] Configurar MapStruct, Lombok e `lombok-mapstruct-binding`.
  - MapStruct gera conversões entre DTOs, domínio e persistência; Lombok reduz código repetitivo e o binding permite o processamento conjunto durante a compilação.
- [x] Configurar JJWT, JaCoCo, Docker Compose e propriedades por ambiente.
  - JJWT assina e valida tokens; JaCoCo coleta cobertura; Docker Compose disponibiliza PostgreSQL para desenvolvimento; configurações externas permitem ajustar conexão e segredos por ambiente.
- [x] Configurar CI, CodeQL, Dependency Review e templates do GitHub.
  - Pipeline verifica build/testes; CodeQL analisa código; Dependency Review avalia dependências em PR; templates padronizam issues e pull requests.

## Fase 1 — Domínio

### Enums e value objects

- [x] Criar enums de certificação, produto, cadeia, transporte, emissão, usuário e auditoria.
  - Valores fechados tipam status de certificação, categoria/unidade de produto, tipo de etapa, modal/combustível, metodologia de cálculo, perfil de usuário e ação auditada; requests inválidos não devem introduzir valores fora dos enums.
- [x] Criar `Cnpj` com validação.
  - Value object centraliza normalização e validação do documento, evitando espalhar a regra pelos casos de uso.
- [x] Criar `EmissionFactor`.
  - Value object representa o fator numérico usado no cálculo de emissão e acompanha a metodologia selecionada.

### Entidades e contratos

- [x] Criar as 11 entidades de domínio.
  - `Address`, `AuditLog`, `Batch`, `CarbonEmission`, `Certification`, `Chain`, `Product`, `Report`, `Supplier`, `Transport` e `User`; relações e invariantes ficam no domínio, sem dependência de JPA.
- [x] Criar uma interface de repositório por entidade.
  - Contratos de persistência para cada entidade permitem aos casos de uso depender do domínio em vez de Spring Data.
- [x] Criar `CarbonFootprintCalculator`, `SustainabilityScoreCalculator` e `PasswordHasher`.
  - Serviços concentram cálculo de pegada, cálculo sob demanda do score e contrato para hash/verificação de senha.
- [x] Criar exceções de domínio.
  - Exceções tipadas representam entidade inexistente, certificação expirada e transição de etapa inválida, derivadas de `DomainException` quando aplicável.

## Fase 2 — Schema e migrations

- [x] Criar migrations das 11 tabelas na ordem das FKs.
  - Schema relacional cobre as entidades de domínio, chaves primárias/estrangeiras e dependências de criação na ordem necessária.
- [x] Tornar endereços de origem/destino da etapa opcionais.
  - Uma etapa sem deslocamento pode ser persistida sem endereço de origem/destino; quando informados, os campos referenciam endereços existentes.
- [x] Criar seeds de desenvolvimento e demonstração.
  - Dados locais permitem exercitar autenticação e fluxos de fornecedores, produtos, lotes, certificações e rastreabilidade sem carga manual inicial.
- [x] Validar execução das migrations contra PostgreSQL.
  - Flyway valida e aplica migrations em PostgreSQL; a inicialização também confirma que o schema está atualizado.

## Fase 3 — Persistência JPA

- [x] Criar as 11 entidades JPA.
  - Mapeamentos representam tabelas, colunas, enums e relações do schema, isolados do modelo de domínio.
- [x] Criar repositórios Spring Data.
  - Interfaces Spring Data oferecem persistência e consultas específicas, incluindo consultas de ranking, relatórios e rastreabilidade.
- [x] Criar implementações dos repositórios de domínio.
  - Adaptadores convertem chamadas dos contratos de domínio para Spring Data e retornam entidades de domínio.
- [x] Criar mappers JPA com MapStruct.
  - Conversões explícitas entre objetos JPA e entidades de domínio evitam expor tipos de persistência à aplicação.
- [x] Validar conversões entre JPA e domínio.
  - Verificações cobrem ida e volta dos dados e relações necessárias aos casos de uso.

## Fase 4 — Aplicação

- [x] Criar DTOs de request/response como records.
  - Contratos tipados incluem requests/responses de endereço, fornecedor, certificação, produto, lote, etapa, transporte, emissão, autenticação, usuário, auditoria e relatório; enums e datas são serializados como valores JSON.
- [x] Criar mappers de aplicação.
  - Mapeadores convertem entre modelos de domínio e DTOs HTTP sem expor entidades JPA.
- [x] Implementar casos de uso de fornecedores e certificações.
  - Operações incluem cadastro/atualização/consulta/listagem/ranking de fornecedores, cadastro e alteração de status de certificação e consulta de certificações próximas do vencimento.
- [x] Implementar casos de uso de produtos e lotes.
  - Produtos podem ser cadastrados, atualizados e listados; lotes são registrados com vínculo a produto e fornecedor e disponibilizados para consulta/rastreabilidade conforme o perfil.
- [x] Implementar casos de uso de etapas, transporte e emissões.
  - Etapas registram responsável e tipo; transporte associa modal, distância, combustível e capacidade; emissão guarda resultado, metodologia e fator utilizado.
- [x] Implementar casos de uso de relatórios.
  - Casos de uso geram relatórios de sustentabilidade por fornecedor/período e permitem consultar relatório individual ou coleção do fornecedor.
- [x] Implementar autenticação, usuários e auditoria.
  - Login valida credenciais e devolve JWT; usuários podem ser criados, consultados pelo próprio token e ter role atualizada por administrador; auditoria registra eventos de escrita.
- [x] Criar exceções de aplicação.
  - Erros de aplicação expressam falhas de fluxo/validação e são traduzidos pela camada Web para respostas HTTP padronizadas.

## Fase 5 — Web API

- [x] Criar controllers para todos os recursos.
  - Base `/api/v1`; grupos incluem autenticação, usuários, fornecedores, certificações, produtos, lotes/etapas, transporte, emissões, relatórios e auditoria. Requests usam JSON e responses usam DTOs de aplicação.
- [x] Criar tratamento global de exceções.
  - Exceções de domínio/aplicação e validação são convertidas em JSON com status HTTP coerente, sem expor detalhes internos inesperados.
- [x] Configurar CORS, Web MVC e OpenAPI.
  - CORS e conversores MVC centralizados; OpenAPI publica a descrição e o Swagger UI para inspeção dos contratos.
- [x] Conferir rotas, DTOs e códigos HTTP contra `spec.md`.
  - Contratos funcionais, permissões e rotas são comparados com a especificação; alterações de contrato devem atualizar documentação e consumidores.

## Fase 6 — Segurança

- [x] Implementar hash BCrypt.
  - Senhas são armazenadas como hash; autenticação compara a senha informada usando o encoder, sem retornar ou registrar senha/hash em DTO.
- [x] Implementar emissão e validação de JWT.
  - Token identifica `userId`, `email` e `role`; assinatura e validade são verificadas antes de aceitar chamadas protegidas.
- [x] Implementar filtro de autenticação.
  - Lê `Authorization: Bearer <token>`, valida o JWT e estabelece o principal de segurança para a requisição.
- [x] Implementar carregamento de usuário.
  - O principal autenticado fornece identificador, email e role necessários aos controllers e casos de uso.
- [x] Configurar autorização por role.
  - Regras restringem operações administrativas e de auditoria, mantendo acesso autenticado ou público conforme a rota.
- [x] Manter login e rastreabilidade/pegada públicas.
  - `POST /api/v1/auth/login`, `GET /api/v1/batches/{batchId}/traceability` e `GET /api/v1/batches/{batchId}/carbon-footprint` não exigem bearer token.
- [x] Testar login, token válido, token ausente e acesso proibido.
  - Cobertura confirma sucesso com credenciais/token válidos e respostas de não autenticado/proibido para as condições correspondentes.

## Fase 7 — Auditoria

- [x] Implementar `AuditLogInterceptor`.
  - Interceptor transversal observa operações anotadas e registra usuário, ação, tabela afetada e horário da execução.
- [x] Garantir que casos de uso não gravem auditoria manualmente.
  - Persistência dos logs fica fora dos fluxos de negócio, evitando duplicidade e dependência de auditoria em cada caso de uso.
- [x] Validar geração automática de log em operações de escrita.
  - Testes verificam que operações instrumentadas produzem registro e que a ação/tabela são identificadas.
- [x] Expor consulta autorizada dos logs.
  - `GET /api/v1/audit-logs` fornece registros a `ADMIN` e `AUDITOR`; response atual usa `logId`, `userId`, `action`, `affectedTable` e `performedAt`.

## Fase 8 — Testes e qualidade

- [x] Cobrir value objects e serviços de domínio.
  - Testes verificam validação de CNPJ e cálculos de pegada/score, incluindo entradas e resultados relevantes para as regras de negócio.
- [x] Cobrir casos de uso com mocks.
  - Dependências de repositório e serviços são isoladas para verificar fluxo, regras, resultados e erros sem banco real.
- [x] Cobrir controllers e persistência com Testcontainers.
  - Testes de integração exercitam contratos HTTP e adaptadores de persistência em PostgreSQL efêmero.
- [x] Cobrir seeds e consultas de relatório.
  - Verificações confirmam carga dos dados de demonstração e resultados das consultas usadas por relatórios.
- [x] Executar `./mvnw --batch-mode verify`.
  - Fase Maven valida compilação, testes e verificações configuradas; a mesma meta é adequada para reprodução local e CI.
- [x] Publicar cobertura JaCoCo no pipeline.
  - CI gera o relatório de cobertura para inspeção junto à execução de verificação.

## Fase 9 — Entregáveis e evolução

- [ ] Formalizar levantamento de requisitos.
- [ ] Formalizar plano de segurança e administração de dados.
- [ ] Criar proposta de arquitetura de dados.
- [ ] Consolidar cronograma no README.
- [ ] Documentar trilha Oracle Academy.
- [ ] Decidir refresh token.
- [ ] Decidir mitigação de enumeração de IDs públicos.
- [ ] Avaliar rate limiting dos endpoints públicos.

## Task 10 — Resumo do dashboard

- [ ] Criar e documentar `GET /api/v1/dashboard/summary`.
  - **Acesso:** usuário autenticado.
  - **Parâmetros:** nenhum.
  - **Resposta `200`:** objeto de resumo com os quatro indicadores já exibidos no dashboard — lotes ativos, certificações expirando, fornecedores ativos e emissão total do mês — e uma lista dos lotes recentes. Cada lote deve conter produto, fornecedor, quantidade e status, suficientes para preencher a tabela da interface.
  - **Schema esperado:**
    ```json
    {
      "activeBatches": 24,
      "expiringCertifications": 5,
      "activeSuppliers": 18,
      "monthlyEmissionKgCo2e": 342.0,
      "recentBatches": [
        {
          "batchId": 101,
          "productName": "Café Orgânico Especial",
          "supplierName": "Fazenda Verde Ltda",
          "quantity": 500.0,
          "unit": "KG",
          "status": "IN_TRANSIT"
        }
      ]
    }
    ```
  - **Erros:** `401` sem autenticação; `500` para falha inesperada.

## Task 11 — Listagem paginada de certificações

- [ ] Implementar e documentar `GET /api/v1/certifications`.
  - **Acesso:** roles `AUDITOR`, `MANAGER` e `ADMIN`, conforme as regras atuais de consulta de certificações.
  - **Query:** `page` zero-based, padrão `0`; `size` padrão `20`, máximo `100`; `onlyExpiring` booleano opcional, padrão `false`, para retornar somente certificações próximas do vencimento.
  - **Consolidação:** absorver o comportamento de `GET /api/v1/certifications/expiring` nesta listagem. Depois que os consumidores migrarem para `GET /api/v1/certifications?onlyExpiring=true`, remover a rota específica antiga.
  - **Resposta `200`:** página com `content` (itens com `certificationId`, `supplierId`, `certification`, `issuingBody`, `issuedAt`, `expiresAt` e `status`), `page`, `size`, `totalElements` e `totalPages`.
  - **Schema esperado:**
    ```json
    {
      "content": [
        {
          "certificationId": 12,
          "supplierId": 4,
          "certification": "ISO 14001",
          "issuingBody": "Certificadora Verde",
          "issuedAt": "2026-01-15",
          "expiresAt": "2027-01-15",
          "status": "ACTIVE"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 42,
      "totalPages": 3
    }
    ```
  - **Validação/erros:** rejeitar página negativa ou tamanho fora de `1..100` com `400`; `401` sem autenticação; `403` sem permissão.

## Task 12 — Listagem paginada de lotes

- [ ] Implementar e documentar `GET /api/v1/batches`.
  - **Acesso:** `ADMIN`, `MANAGER` e `AUDITOR` podem listar todos os lotes; `SUPPLIER` pode consultar somente os próprios lotes.
  - **Query:** `page` zero-based, padrão `0`; `size` padrão `20`, máximo `100`; `supplierId` opcional para restringir a página aos lotes de um fornecedor.
  - **Consolidação:** absorver `GET /api/v1/suppliers/{supplierId}/batches` usando o filtro `supplierId`. Preservar o acesso do perfil `SUPPLIER` somente aos próprios lotes, independentemente do filtro informado; perfis administrativos mantêm a consulta global e podem filtrar por fornecedor. Remover a rota aninhada após a migração dos consumidores.
  - **Resposta `200`:** página com `content` (itens com `batchId`, `productId`, `productName`, `supplierId`, `supplierName`, `quantity` e `producedAt`), `page`, `size`, `totalElements` e `totalPages`.
  - **Schema esperado:**
    ```json
    {
      "content": [
        {
          "batchId": 101,
          "productId": 8,
          "productName": "Café Orgânico Especial",
          "supplierId": 4,
          "supplierName": "Fazenda Verde Ltda",
          "quantity": 500.0,
          "producedAt": "2026-09-20"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 57,
      "totalPages": 3
    }
    ```
  - **Validação/erros:** rejeitar página negativa ou tamanho fora de `1..100` com `400`; `401` sem autenticação; `403` sem role autorizada.

## Task 13 — Ranking de fornecedores paginado

- [ ] Atualizar e documentar `GET /api/v1/suppliers/ranking`.
  - **Acesso:** usuário autenticado; preservar a ordenação atual do ranking.
  - **Query:** `limit` padrão `20`, máximo `100`; `offset` padrão `0`; `search` opcional. Aplicar o termo antes da paginação para localizar fornecedores por nome (full-text em português) ou CNPJ (correspondência parcial após normalizar para dígitos).
  - **Busca e índices:** criar uma migration Flyway com coluna `tsvector` gerada a partir do nome (configuração `portuguese`) e índice GIN para full-text; habilitar `pg_trgm` e criar índice GIN trigram para busca parcial de CNPJ normalizado. A consulta por nome deve usar `websearch_to_tsquery` usando a configuração de idioma `portuguese` e o parâmetro `:search` contra o `tsvector`; a busca de CNPJ deve comparar somente dígitos. Combinar os resultados com `OR`, sem duplicar fornecedores.
  - **Resposta `200`:** objeto `{ items, limit, offset, hasNext }`; cada item contém `supplierId`, `name`, `sustainabilityScore`, `activeCertificationCount` e `totalCo2Kg`. `hasNext` indica se existe ao menos mais um resultado após o intervalo retornado.
  - **Schema esperado:**
    ```json
    {
      "items": [
        {
          "supplierId": 4,
          "name": "Fazenda Verde Ltda",
          "sustainabilityScore": 92.5,
          "activeCertificationCount": 3,
          "totalCo2Kg": 125.75
        }
      ],
      "limit": 20,
      "offset": 0,
      "hasNext": true
    }
    ```
  - **Validação/erros:** rejeitar `limit` fora de `1..100` ou `offset` negativo com `400`; `401` sem autenticação.

## Task 14 — Produtos com busca e paginação

- [ ] Atualizar e documentar `GET /api/v1/products`.
  - **Acesso:** preservar as permissões atuais da listagem de produtos.
  - **Query:** `limit` padrão `20`, máximo `100`; `offset` padrão `0`; `search` opcional, aplicado antes da paginação para buscar por nome, categoria ou descrição usando full-text em português.
  - **Busca e índices:** criar migration Flyway com coluna `tsvector` gerada a partir de nome e descrição (tratando descrição nula, com configuração `portuguese`) e índice GIN; tratar categoria separadamente, comparando também seu código/label de domínio sem diferenciar maiúsculas de minúsculas. Usar `websearch_to_tsquery` usando a configuração de idioma `portuguese` e o parâmetro `:search` na consulta do repository; palavras informadas em qualquer ordem devem poder encontrar o produto; a categoria deve aceitar os termos apresentados na interface e seus códigos da API.
  - **Resposta `200`:** objeto `{ items, limit, offset, hasNext }`; cada item contém `productId`, `name`, `category`, `unit` e `description`.
  - **Schema esperado:**
    ```json
    {
      "items": [
        {
          "productId": 8,
          "name": "Café Orgânico Especial",
          "category": "AGRICULTURE",
          "unit": "KG",
          "description": "Café produzido com práticas agroecológicas."
        }
      ],
      "limit": 20,
      "offset": 0,
      "hasNext": false
    }
    ```
  - **Validação/erros:** rejeitar `limit` fora de `1..100` ou `offset` negativo com `400`; `401` sem autenticação e `403` sem permissão.

## Task 15 — Auditoria filtrada e paginada

- [ ] Atualizar e documentar `GET /api/v1/audit-logs`.
  - **Acesso:** roles `ADMIN` e `AUDITOR`.
  - **Query:** `from` e `to` obrigatórios, em formato `YYYY-MM-DD` e inclusivos; `action` e `userEmail` opcionais; `limit` padrão `20`, máximo `100`; `offset` padrão `0`. O email deve ser correspondência parcial case-insensitive.
  - **Busca e índices:** criar migration Flyway com índice GIN `pg_trgm` sobre o email do usuário associado ao log (ou coluna de email persistida no log, se esse for o modelo adotado), para acelerar `ILIKE` com curingas antes e depois do termo; evitar full-text para email, pois pontuação e fragmentos de endereço precisam ser preservados.
  - **Resposta `200`:** objeto `{ items, limit, offset, hasNext }`; cada item contém `logId`, `userId`, `userEmail`, `action`, `affectedTable` e `performedAt` em ISO 8601. `hasNext` indica se há mais registros após o intervalo retornado.
  - **Schema esperado:**
    ```json
    {
      "items": [
        {
          "logId": 9001,
          "userId": 7,
          "userEmail": "ana.souza@empresa.com",
          "action": "UPDATE",
          "affectedTable": "suppliers",
          "performedAt": "2026-09-26T14:35:12"
        }
      ],
      "limit": 20,
      "offset": 0,
      "hasNext": true
    }
    ```
  - **Validação/erros:** `400` para datas ausentes/inválidas, `from` posterior a `to`, ação inválida, `limit` fora de `1..100` ou `offset` negativo; `401` sem autenticação; `403` sem role autorizada.

## Task 16 — Listagem paginada de usuários

- [ ] Implementar e documentar `GET /api/v1/users`.
  - **Acesso:** somente role `ADMIN`.
  - **Query:** `email` opcional para filtrar por correspondência parcial, sem diferenciar maiúsculas de minúsculas; `limit` padrão `20`, máximo `100`; `offset` padrão `0`.
  - **Busca e índices:** criar migration Flyway habilitando `pg_trgm` e um índice GIN trigram sobre o email normalizado; usar correspondência parcial case-insensitive (`ILIKE` com curingas antes e depois do termo). Não usar full-text para endereços de email.
  - **Resposta `200`:** objeto `{ items, limit, offset, hasNext }`; cada item contém `userId`, `name`, `email`, `role` e `createdAt` em ISO 8601. `hasNext` indica se existe outro usuário após o intervalo retornado.
  - **Schema esperado:**
    ```json
    {
      "items": [
        {
          "userId": 7,
          "name": "Ana Souza",
          "email": "ana.souza@empresa.com",
          "role": "ADMIN",
          "createdAt": "2026-09-20T10:00:00"
        }
      ],
      "limit": 20,
      "offset": 0,
      "hasNext": false
    }
    ```
  - **Validação/erros:** rejeitar `limit` fora de `1..100` ou `offset` negativo com `400`; `401` sem autenticação; `403` para usuário sem role `ADMIN`.

## Task 17 — Gestão de relatórios paginada e consulta individual

- [ ] Criar e documentar uma listagem geral paginada de relatórios e completar o contrato de consulta individual para a tela de gestão do frontend.
  - **Listagem:** adicionar `GET /api/v1/reports`, acessível a `ADMIN`, `MANAGER` e `AUDITOR` para consulta global; `SUPPLIER` pode consultar somente os próprios relatórios. Aceitar `limit` padrão `20` (máximo `100`), `offset` padrão `0` e `supplierId` opcional para filtrar os relatórios de um fornecedor.
  - **Consolidação:** absorver `GET /api/v1/suppliers/{supplierId}/reports` com o filtro `supplierId`. Preservar a consulta de fornecedor ao perfil `SUPPLIER` somente para os próprios relatórios; perfis administrativos podem consultar todos ou filtrar por fornecedor. Remover a rota aninhada após a migração dos consumidores.
  - **Resposta `200` da listagem:** objeto `{ items, limit, offset, hasNext }`. Cada item inclui `reportId`, `supplierId`, `supplierCnpj`, `supplierName`, `periodStartAt`, `periodEndAt`, `totalCo2Kg`, `totalBatchCount` e `generatedAt`. O CNPJ e a razão social vêm do fornecedor associado; `totalBatchCount` representa o total de lotes considerados no relatório, não a quantidade de produtos rastreados já representada por `trackedProductCount`.
  - **Detalhe:** manter `GET /api/v1/reports/{reportId}` e enriquecer seu response com identificação do fornecedor (`supplierCnpj`, `supplierName`) e `totalBatchCount`, além dos campos atuais `reportId`, `supplierId`, `periodStartAt`, `periodEndAt`, `totalCo2Kg`, `trackedProductCount` e `generatedAt`.
  - **Schema da listagem:**
    ```json
    {
      "items": [
        {
          "reportId": 301,
          "supplierId": 4,
          "supplierCnpj": "12.345.678/0001-90",
          "supplierName": "Fazenda Verde Ltda",
          "periodStartAt": "2026-08-01",
          "periodEndAt": "2026-08-31",
          "totalCo2Kg": 125.75,
          "totalBatchCount": 12,
          "generatedAt": "2026-09-01T10:30:00"
        }
      ],
      "limit": 20,
      "offset": 0,
      "hasNext": true
    }
    ```
  - **Schema do detalhe:**
    ```json
    {
      "reportId": 301,
      "supplierId": 4,
      "supplierCnpj": "12.345.678/0001-90",
      "supplierName": "Fazenda Verde Ltda",
      "periodStartAt": "2026-08-01",
      "periodEndAt": "2026-08-31",
      "totalCo2Kg": 125.75,
      "totalBatchCount": 12,
      "trackedProductCount": 5,
      "generatedAt": "2026-09-01T10:30:00"
    }
    ```
  - **Validação/erros:** rejeitar `limit` fora de `1..100` ou `offset` negativo com `400`; responder `401` sem autenticação, `403` sem permissão e `404` quando o relatório solicitado não existir.
