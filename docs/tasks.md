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
  - `GET /api/v1/audit-logs` fornece registros a `ADMIN` e `AUDITOR`; o response atual inclui `logId`, `userId`, `userEmail`, `action`, `affectedTable` e `performedAt`.

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

- [x] Criar e documentar `GET /api/v1/dashboard/summary`.
  - **Acesso:** qualquer usuário autenticado. O resumo é global para todos os perfis, inclusive `SUPPLIER`.
  - **Query:** `limit` opcional para a lista de lotes recentes; padrão `10` quando omitido ou nulo; valores aceitos de `1` a `100`.
  - **Indicadores:** `activeBatches` conta lotes que têm ao menos uma etapa em `chain` e cuja etapa mais recente não é `RETAIL`. A etapa mais recente é determinada por `startedAt`, com `chainId` como desempate. Lotes sem etapas não contam como ativos. `expiringCertifications` conta certificações com `expiresAt` de hoje até hoje mais 30 dias, inclusive, sem filtro por status. `suppliers` conta todos os fornecedores cadastrados, pois o domínio ainda não possui estado ativo/inativo. `monthlyEmissionKgCo2e` soma `co2Kg` das emissões cujo `calculatedAt` está no mês calendário atual; sem emissões, retorna `0`.
  - **Lotes recentes:** ordenados por `producedAt` decrescente e `batchId` decrescente como desempate. Cada item contém produto, fornecedor, quantidade, unidade e `status` baseado no `stage_type` mais recente. Lotes sem `chain` não aparecem por não possuírem status; lotes em `RETAIL` podem aparecer nessa lista, embora não contem como ativos.
  - **Resposta `200`:** objeto com os quatro indicadores e a lista de lotes recentes.
  - **Schema esperado:**
    ```json
    {
      "activeBatches": 24,
      "expiringCertifications": 5,
      "suppliers": 18,
      "monthlyEmissionKgCo2e": 342.0,
      "recentBatches": [
        {
          "batchId": 101,
          "productName": "Café Orgânico Especial",
          "supplierName": "Fazenda Verde Ltda",
          "quantity": 500.0,
          "unit": "KG",
          "status": "TRANSPORT"
        }
      ]
    }
    ```
  - **Erros:** `400` para `limit` fora de `1..100`; `401` sem autenticação; `500` para falha inesperada.

## Task 11 — Listagem paginada de certificações

- [x] Implementar e documentar `GET /api/v1/certifications`.
  - **Acesso:** roles `AUDITOR`, `MANAGER` e `ADMIN`, conforme as regras atuais de consulta de certificações.
  - **Query:** `page` zero-based, padrão `0`; `size` padrão `20`, máximo `100`; `onlyExpiring` booleano opcional, padrão `false`, para retornar certificações com vencimento entre hoje e os próximos 30 dias, inclusive, sem filtrar pelo status.
  - **Ordenação:** `expiresAt` crescente, com `certificationId` crescente como desempate, para páginas estáveis.
  - **Consolidação:** a nova listagem absorve a consulta de certificações próximas do vencimento por meio de `onlyExpiring=true`. A rota `GET /api/v1/certifications/expiring` permanece temporariamente por ainda ser consumida pelo frontend; removê-la após a migração desse consumidor.
  - **Resposta `200`:** página com `content` (itens com `certificationId`, `supplierId`, `certification`, `issuingBody`, `issuedAt`, `expiresAt` e `status`), `page`, `size`, `totalElements` e `totalPages`. Quando não houver resultados, `content` é vazio e `totalPages` é `0`.
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
  - **Validação/erros:** rejeitar página negativa, tamanho fora de `1..100` ou valor inválido de `onlyExpiring` com `400`; `401` sem autenticação; `403` sem permissão.

## Task 12 — Listagem paginada de lotes

- [x] Implementar e documentar `GET /api/v1/batches`.
  - **Acesso:** `ADMIN`, `MANAGER` e `AUDITOR` podem listar todos os lotes; `SUPPLIER` consulta somente os próprios. Como o modelo atual não possui associação explícita entre usuário e fornecedor, esta implementação adota a convenção de que o `userId` do token `SUPPLIER` é igual ao `supplierId`; o servidor ignora `supplierId` enviado por esse perfil.
  - **Query:** `page` zero-based, padrão `0`; `size` padrão `20`, máximo `100`; `supplierId` opcional para restringir a página aos lotes de um fornecedor.
  - **Consolidação:** absorver `GET /api/v1/suppliers/{supplierId}/batches` usando o filtro `supplierId`. O frontend foi migrado para a rota paginada e a rota aninhada foi removida. `SUPPLIER` recebe sempre o filtro do próprio token, independentemente do query param; `ADMIN`, `MANAGER` e `AUDITOR` mantêm consulta global e podem filtrar por fornecedor.
  - **Ordenação:** `producedAt` decrescente, com `batchId` decrescente como desempate.
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

- [x] Atualizar e documentar `GET /api/v1/suppliers/ranking`.
  - **Acesso:** usuário autenticado; preservar a ordenação atual do ranking.
  - **Query:** `limit` padrão `20`, máximo `100`; `offset` padrão `0`; `search` opcional. Aplicar o termo antes da paginação para localizar fornecedores por nome (full-text em português) ou CNPJ (correspondência parcial após normalizar para dígitos).
  - **Busca e índices:** criar uma migration Flyway com coluna `tsvector` gerada a partir do nome (configuração `portuguese`) e índice GIN para full-text; habilitar `pg_trgm` e criar índice GIN trigram para busca parcial de CNPJ normalizado. A consulta por nome deve usar `websearch_to_tsquery` usando a configuração de idioma `portuguese` e o parâmetro `:search` contra o `tsvector`; a busca de CNPJ deve comparar somente dígitos. Combinar os resultados com `OR`, sem duplicar fornecedores.
  - **Ordenação:** manter `sustainabilityScore` decrescente e usar `supplierId` crescente como desempate estável; aplicar `offset` e `limit` após o ranking dos fornecedores encontrados.
  - **Resposta `200`:** objeto `{ items, limit, offset, hasNext, totalPages }`; cada item contém `supplierId`, `name`, `sustainabilityScore`, `activeCertificationCount` e `totalCo2Kg`. `hasNext` indica se existe ao menos mais um resultado após o intervalo retornado; `totalPages` considera todos os fornecedores que correspondem à busca.
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
      "hasNext": true,
      "totalPages": 3
    }
    ```
  - **Validação/erros:** rejeitar `limit` fora de `1..100` ou `offset` negativo com `400`; `401` sem autenticação.

## Task 14 — Produtos com busca e paginação

- [x] Atualizar e documentar `GET /api/v1/products`.
  - **Acesso:** preservar as permissões atuais da listagem de produtos.
  - **Query:** `limit` padrão `20`, máximo `100`; `offset` padrão `0`; `search` opcional, aplicado antes da paginação para buscar por nome, categoria ou descrição usando full-text em português.
  - **Busca e índices:** criar migration Flyway com coluna `tsvector` gerada a partir de nome e descrição (tratando descrição nula, com configuração `portuguese`) e índice GIN; tratar categoria separadamente, comparando também seu código/label de domínio sem diferenciar maiúsculas de minúsculas. Usar `websearch_to_tsquery` usando a configuração de idioma `portuguese` e o parâmetro `:search` na consulta do repository; palavras informadas em qualquer ordem devem poder encontrar o produto; a categoria deve aceitar os termos apresentados na interface e seus códigos da API.
  - **Categorias aceitas:** `AGRICULTURE`/`Agricultura`, `LIVESTOCK`/`Pecuária`, `PROCESSED_FOOD`/`Alimentos processados`, `TEXTILE`/`Têxtil`, `FORESTRY`/`Florestal` e `OTHER`/`Outro`; comparação ignora caixa e acentos.
  - **Ordenação/paginação:** ordenar por `productId` crescente e buscar `limit + 1` registros para determinar `hasNext`; calcular `totalPages` com a contagem total que corresponde à busca aplicada.
  - **Resposta `200`:** objeto `{ items, limit, offset, hasNext, totalPages }`; cada item contém `productId`, `name`, `category`, `unit` e `description`.
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
      "hasNext": false,
      "totalPages": 1
    }
    ```
  - **Validação/erros:** rejeitar `limit` fora de `1..100` ou `offset` negativo com `400`; `401` sem autenticação e `403` sem permissão.

## Task 15 — Auditoria filtrada e paginada

- [x] Atualizar e documentar `GET /api/v1/audit-logs`.
  - **Acesso:** roles `ADMIN` e `AUDITOR`.
  - **Query:** `from` e `to` obrigatórios, em formato `YYYY-MM-DD` e inclusivos; `action` e `userEmail` opcionais; `limit` padrão `20`, máximo `100`; `offset` padrão `0`. O email deve ser correspondência parcial case-insensitive.
  - **Busca e índices:** criar migration Flyway com índice GIN `pg_trgm` sobre o email do usuário associado ao log (ou coluna de email persistida no log, se esse for o modelo adotado), para acelerar `ILIKE` com curingas antes e depois do termo; evitar full-text para email, pois pontuação e fragmentos de endereço precisam ser preservados.
  - **Ordenação/paginação:** ordenar por `performedAt` decrescente e `logId` decrescente como desempate; aplicar `limit + 1` no banco para calcular `hasNext` e contar os registros após todos os filtros para calcular `totalPages`.
  - **Resposta `200`:** objeto `{ items, limit, offset, hasNext, totalPages }`; cada item contém `logId`, `userId`, `userEmail`, `action`, `affectedTable` e `performedAt` em ISO 8601. `hasNext` indica se há mais registros após o intervalo retornado.
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
      "hasNext": true,
      "totalPages": 2
    }
    ```
  - **Validação/erros:** `400` para datas ausentes/inválidas, `from` posterior a `to`, ação inválida, `limit` fora de `1..100` ou `offset` negativo; `401` sem autenticação; `403` sem role autorizada.

## Task 16 — Listagem paginada de usuários

- [x] Implementar e documentar `GET /api/v1/users`.
  - **Acesso:** somente role `ADMIN`.
  - **Query:** `email` opcional para filtrar por correspondência parcial, sem diferenciar maiúsculas de minúsculas; `limit` padrão `20`, máximo `100`; `offset` padrão `0`.
  - **Busca e índices:** criar migration Flyway habilitando `pg_trgm` e um índice GIN trigram sobre o email normalizado; usar correspondência parcial case-insensitive (`ILIKE` com curingas antes e depois do termo). Não usar full-text para endereços de email.
  - **Resposta `200`:** objeto `{ items, limit, offset, hasNext, totalPages }`; cada item contém `userId`, `name`, `email`, `role` e `createdAt` em ISO 8601. `hasNext` indica se existe outro usuário após o intervalo retornado; `totalPages` considera o filtro de email quando informado.
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
      "hasNext": false,
      "totalPages": 1
    }
    ```
  - **Validação/erros:** rejeitar `limit` fora de `1..100` ou `offset` negativo com `400`; `401` sem autenticação; `403` para usuário sem role `ADMIN`.

## Task 17 — Gestão de relatórios paginada e consulta individual

- [x] Criar e documentar uma listagem geral paginada de relatórios e completar o contrato de consulta individual para a tela de gestão do frontend.
  - **Listagem:** adicionar `GET /api/v1/reports`, acessível a `ADMIN`, `MANAGER` e `AUDITOR` para consulta global; `SUPPLIER` pode consultar somente os próprios relatórios. Aceitar `limit` padrão `20` (máximo `100`), `offset` padrão `0` e `supplierId` opcional para filtrar os relatórios de um fornecedor.
  - **Consolidação:** `GET /api/v1/reports` absorve a listagem por fornecedor com o filtro `supplierId`. Preservar a consulta de fornecedor ao perfil `SUPPLIER` somente para os próprios relatórios; perfis administrativos podem consultar todos ou filtrar por fornecedor. A antiga rota aninhada `GET /api/v1/suppliers/{supplierId}/reports` foi removida.
  - **Resposta `200` da listagem:** objeto `{ items, limit, offset, hasNext, totalPages }`. Cada item inclui `reportId`, `supplierId`, `supplierCnpj`, `supplierName`, `periodStartAt`, `periodEndAt`, `totalCo2Kg`, `totalBatchCount` e `generatedAt`. O CNPJ e a razão social vêm do fornecedor associado; `totalBatchCount` representa o total de lotes considerados no relatório, não a quantidade de produtos rastreados já representada por `trackedProductCount`. `totalPages` deve considerar o filtro de fornecedor e as regras de autorização aplicadas.
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
      "hasNext": true,
      "totalPages": 2
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

## Task 18 — Ampliação dos dados de demonstração

- [x] Criar migrations Flyway adicionais para popular o banco de desenvolvimento e demonstração com dados variados e coerentes, simulando uso frequente do frontend.
  - **Usuários:** inserir pelo menos 11 usuários adicionais, com nomes, emails únicos e distribuição representativa entre as roles disponíveis. Credenciais de demonstração devem seguir o mecanismo de hash da aplicação e não armazenar senhas em texto puro.
  - **Produtos e categorias:** inserir pelo menos 40 produtos com nomes, descrições, unidades e categorias variados. Avaliar a inclusão de novas categorias; se forem adicionadas, atualizar de forma compatível os enums, validações, schema, filtros/busca e documentação da API.
  - **Fornecedores:** inserir pelo menos 30 fornecedores adicionais, com CNPJs válidos e únicos, endereços, contatos e pontuações consistentes com os dados associados.
  - **Cobertura relacional:** criar novos registros de lotes, etapas/cadeias, transportes, emissões de carbono, certificações, relatórios e auditoria, além de quaisquer outras tabelas necessárias. Distribuir as relações entre os novos e os registros existentes, mantendo todas as FKs válidas e os cálculos/intervalos coerentes.
  - **Cenários consultáveis:** incluir certificações vencidas, próximas do vencimento (dentro da janela de 30 dias) e válidas por período maior; gerar também lotes em diferentes etapas, emissões no mês atual e relatórios de períodos variados. As consultas de dashboard, certificações expirando, rastreabilidade, ranking, busca/listagem de produtos e relatórios devem retornar resultados representativos.
  - **Migrations e ambiente:** manter os seeds reproduzíveis e compatíveis com um banco criado do zero e com as migrations já aplicadas; não depender de IDs fixos que conflitem com os dados existentes, nem duplicar registros ao reiniciar a aplicação. Restringir dados fictícios ao contexto de desenvolvimento/demonstração, conforme a estratégia de configuração do projeto.
  - **Validação:** confirmar que a sequência Flyway aplica em PostgreSQL limpo e existente e que as rotas relevantes conseguem consultar os novos cenários sem erros de integridade ou incompatibilidade de enum.

## Task 19 — Filtro de status e remoção da rota de certificações expirando

- [x] Atualizar a listagem de certificações para filtrar por status e remover a rota dedicada a certificações expirando.
  - **Rota removida:** eliminar `GET /api/v1/certifications/expiring` e sua implementação associada.
  - **Query:** `GET /api/v1/certifications` mantém `page` zero-based (padrão `0`) e `size` (padrão `20`, máximo `100`), remove o parâmetro `onlyExpiring` e aceita `status` opcional com qualquer valor válido de `CertificationStatus` (`ACTIVE`, `EXPIRED`, `SUSPENDED` ou `UNDER_REVIEW`). Quando informado, retornar apenas certificações com esse status.
  - **Sem filtro:** quando `status` não for informado, retornar certificações de todos os status, ordenadas por `issued_at` decrescente para apresentar as mais recentes primeiro; usar `certificationId` decrescente como desempate para manter a paginação estável.
  - **Resposta:** preservar o formato paginado atual, com `content` (itens contendo `certificationId`, `supplierId`, `certification`, `issuingBody`, `issuedAt`, `expiresAt` e `status`), `page`, `size`, `totalElements` e `totalPages`.
  - **Validação/erros:** rejeitar página negativa, tamanho fora de `1..100` ou status inválido com `400`; responder `401` sem autenticação e `403` sem permissão.
  - **Escopo de documentação:** não é necessário atualizar a documentação do frontend.

## Task 20 — Campo `totalPages` em todas as rotas paginadas

- [x] Padronizar as respostas de todas as rotas paginadas para incluir o campo `totalPages`.
  - **Rotas:** `GET /api/v1/batches` e `GET /api/v1/certifications` (paginação `page`/`size`); `GET /api/v1/users`, `GET /api/v1/suppliers/ranking`, `GET /api/v1/products`, `GET /api/v1/audit-logs` e `GET /api/v1/reports` (paginação `limit`/`offset`). Considerar os filtros opcionais de cada rota no cálculo dos totais.
  - **Resposta:** incluir `totalPages` no objeto de paginação de cada rota, calculado como o número total de páginas para o tamanho solicitado; retornar `0` quando não houver resultados. Preservar os demais campos e parâmetros existentes, como `content` ou `items`, `page`, `size`, `limit`, `offset`, `totalElements` e `hasNext`.
  - **Consistência:** garantir que `totalPages` reflita os filtros aplicados e que todas as respostas paginadas usem a mesma regra de cálculo.
  - **Documentação:** atualizar os contratos e exemplos das rotas afetadas para mostrar `totalPages`.

## Task 21 — Unificação da consulta individual de relatórios

- [ ] Remover `GET /api/v1/reports/{reportId}` e incorporar a consulta individual em `GET /api/v1/reports` por meio do parâmetro opcional `reportId`.
  - **Listagem:** quando `reportId` não for informado, preservar a listagem paginada atual e seus filtros, parâmetros, formato de resposta e regras de autorização.
  - **Consulta individual:** quando `reportId` for informado, retornar somente o relatório correspondente no formato de detalhe atual, incluindo `reportId`, `supplierId`, `supplierCnpj`, `supplierName`, `periodStartAt`, `periodEndAt`, `totalCo2Kg`, `totalBatchCount`, `trackedProductCount` e `generatedAt`.
  - **Autorização e erros:** preservar as permissões atuais para relatórios; `SUPPLIER` só pode consultar relatório próprio. Rejeitar `reportId` inválido com `400`, responder `404` quando não existir relatório correspondente e manter `401`/`403` para falhas de autenticação/autorização.
  - **Remoção:** excluir a rota `GET /api/v1/reports/{reportId}` e sua implementação associada; atualizar OpenAPI, documentação e contratos para usar `GET /api/v1/reports?reportId={reportId}`.

## Task 22 — Consolidação das consultas de fornecedores

- [ ] Concentrar as consultas de fornecedores em `GET /api/v1/suppliers` e remover as rotas GET redundantes.
  - **Parâmetros:** adicionar `supplierId` opcional para consultar um fornecedor específico e `ranked` opcional (booleano) para solicitar a listagem ranqueada, preservando os filtros e a paginação do ranking atual.
  - **Comportamento:** sem `supplierId` e sem `ranked=true`, preservar a listagem atual de fornecedores; com `supplierId`, retornar os detalhes do fornecedor; com `ranked=true`, retornar a listagem ranqueada no formato paginado atual.
  - **Conflito:** os parâmetros `supplierId` e `ranked` são mutuamente exclusivos; rejeitar com `400` qualquer requisição que informe ambos, mesmo quando `ranked=false`. Validar também os valores inválidos dos parâmetros.
  - **Remoção de rotas:** remover `GET /api/v1/suppliers/{supplierId}` e `GET /api/v1/suppliers/ranking`, incluindo implementações, autorização e documentação associadas. As rotas de relatórios seguem a Task 21: manter `GET /api/v1/reports` para listagem paginada e detalhe via `reportId`, removendo somente `GET /api/v1/reports/{reportId}`. Manter a geração de relatórios por `POST /api/v1/suppliers/{supplierId}/reports`.
  - **Compatibilidade:** preservar as regras de autorização, filtros, ordenação, paginação e formatos de resposta existentes para cada comportamento que passar a ser atendido por `GET /api/v1/suppliers`; atualizar OpenAPI e contratos da API.

## Task 23 — Detalhamento dos eventos de auditoria

- [ ] Registrar a entidade afetada e os dados alterados nos eventos de auditoria, expondo essas informações na rota de consulta.
  - **Schema:** criar migration Flyway aditiva para incluir em `audit_log` o ID da entidade e os dados anteriores/posteriores em `JSONB`. O schema resultante deve conter:
    ```sql
    affected_entity_id BIGINT,
    before_data JSONB,
    after_data JSONB
    ```
    Criar também índice por `(affected_table, affected_entity_id)`. Manter as novas colunas anuláveis para preservar registros históricos que não tenham esses detalhes recuperáveis; novos eventos devem preencher `affected_entity_id`.
  - **Identificação:** preencher o ID primário da entidade afetada para `INSERT`, `UPDATE`, `DELETE` e `STATUS_CHANGE`; resolver o tipo de entidade de forma explícita e confiável, sem selecionar arbitrariamente um getter terminado em `Id`.
  - **Conteúdo:** para `UPDATE` e `STATUS_CHANGE`, armazenar em `before_data` e `after_data` objetos JSON com somente os campos alterados e seus valores antes/depois. Para `INSERT`, `before_data` é `NULL` e `after_data` contém os dados auditáveis criados; para `DELETE`, `before_data` contém os dados auditáveis anteriores à exclusão e `after_data` é `NULL`. Representar associações por seus IDs, sem serializar grafos de entidades.
  - **Privacidade:** excluir senhas, hashes, tokens e outros segredos dos JSONB. Não inventar valores anteriores, posteriores ou IDs que não possam ser recuperados.
  - **Ações:** garantir a classificação correta entre `INSERT`, `UPDATE`, `DELETE` e `STATUS_CHANGE`, incluindo a detecção de mudanças de status em vez de registrar toda alteração como `UPDATE`.
  - **Contrato da rota:** manter `GET /api/v1/audit-logs?from={date}&to={date}&action={action}&userEmail={fragment}&limit=20&offset=0` e os filtros/autorização atuais. Cada item deve incluir `logId`, `userId`, `userEmail`, `action`, `affectedTable`, `affectedEntityId`, `beforeData`, `afterData` e `performedAt`. `beforeData` e `afterData` são objetos JSON ou `null`; preservar a paginação da rota, incluindo `items`, `limit`, `offset`, `hasNext` e `totalPages`.
    ```json
    {
      "items": [
        {
          "logId": 981,
          "userId": 7,
          "userEmail": "ana.souza@empresa.com",
          "action": "UPDATE",
          "affectedTable": "suppliers",
          "affectedEntityId": 42,
          "beforeData": { "name": "Fazenda Verde" },
          "afterData": { "name": "Fazenda Verde Ltda" },
          "performedAt": "2026-09-30T14:32:10"
        }
      ],
      "limit": 20,
      "offset": 0,
      "hasNext": false,
      "totalPages": 1
    }
    ```
  - **Exemplo de row:** um evento de atualização de fornecedor deve ser armazenado com `affected_table = 'suppliers'`, `affected_entity_id = 42`, `before_data = '{"name":"Fazenda Verde"}'::jsonb` e `after_data = '{"name":"Fazenda Verde Ltda"}'::jsonb`, além de `log_id`, `user_id`, `action = 'UPDATE'` e `performed_at`.
  - **Dados existentes:** revisar e corrigir inconsistências dos registros de auditoria que possam ser determinadas a partir dos dados disponíveis; a migration deve preservar o histórico e deixar nulos os detalhes antigos irrecuperáveis, sem fabricar snapshots.
  - **Documentação:** atualizar OpenAPI, contratos e exemplos da API para refletir as colunas, campos de resposta e formato JSONB.
  - **Validação:** verificar a migration em banco limpo e em banco com logs existentes, e conferir que inclusões, atualizações, mudanças de status e exclusões gerem o ID e os dados de antes/depois esperados.
