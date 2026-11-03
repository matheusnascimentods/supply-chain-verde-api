# Plano técnico — Empresas, planos e isolamento por companhia

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Banco (V7–V9)

São três migrations, na ordem pedida: criar, mover e apagar. Toda regra de tipo usa o mesmo truque de `chain.previous_stage_type` (V1): uma coluna gerada constante entra numa FK composta.

### V7 — `create_plan_and_company`

```sql
CREATE TABLE plan (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    code      ENUM('BASIC', 'PRO', 'PRO_MAX') COLLATE utf8mb4_0900_as_ci NOT NULL,
    name      VARCHAR(60) NOT NULL,
    max_users INT NOT NULL,
    created_at/updated_at ...,
    CONSTRAINT uq_plan_code UNIQUE (code),
    CONSTRAINT chk_plan_max_users_positive CHECK (max_users > 0)
);
INSERT INTO plan (code, name, max_users) VALUES ('BASIC','Basic',3), ('PRO','Pro',10), ('PRO_MAX','Pro Max',15);

CREATE TABLE company (
    id, name, cnpj, address_id, phone,             -- iguais a supplier
    email VARCHAR(180) NOT NULL,
    type  ENUM('MARKET', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci NOT NULL,
    created_at/updated_at ...,
    CONSTRAINT uq_company_cnpj UNIQUE (cnpj),
    CONSTRAINT uq_company_id_type UNIQUE (id, type),   -- alvo das FKs compostas
    FK address, INDEX name, FULLTEXT ft_company_name (name), INDEX type
);

CREATE TABLE company_plan (
    id, company_id, plan_id,
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at   DATETIME(6) NULL,
    active_company_id BIGINT GENERATED ALWAYS AS (IF(ended_at IS NULL, company_id, NULL)) STORED,
    CONSTRAINT uq_company_plan_active UNIQUE (active_company_id),   -- RN-03: uma ativa
    CONSTRAINT chk_company_plan_period CHECK (ended_at IS NULL OR ended_at >= started_at),
    FKs company, plan
);
```

### V8 — `move_supplier_to_company`

1. `INSERT INTO company (id, ...) SELECT id, ..., 'SUPPLIER' FROM supplier`. O **id é mantido** (RN-24), então `batch`, `certification` e `report` continuam válidos sem `UPDATE`. O `email` vem do usuário vinculado; sem usuário, usa `CONCAT('fornecedor', id, '@supplychainverde.local')`.
2. Cria a companhia `MARKET` de demonstração, com id `MAX(id) + 1` e endereço novo, e as assinaturas: `PRO_MAX` para a demo e `BASIC` para os fornecedores.
3. `users`:
   - Adiciona `company_id` NULL com FK.
   - Adiciona `PLATFORM_ADMIN` ao ENUM de `role`.
   - Preenche os dados conforme RN-24 a RN-26. Usuário `SUPPLIER` vinculado vira `role = 'ADMIN'` e recebe `company_id = supplier.id`.
   - Adiciona `CHECK ((role = 'PLATFORM_ADMIN') = (company_id IS NULL))` e `INDEX (company_id, role)`, usado pela contagem de admins e de usuários.
4. `product`:
   - Adiciona `supplier_id`.
   - Duplica o que for necessário (RN-28) usando uma tabela temporária de pares `(product_id, supplier_id)` tirados de `batch`. O fornecedor de menor id fica com a linha original, os outros recebem cópias, e `batch.product_id` é reapontado.
   - Depois, `supplier_id NOT NULL`, `supplier_type ... GENERATED ALWAYS AS ('SUPPLIER') STORED`, FK `(supplier_id, supplier_type) → company(id, type)` e `UNIQUE (id, supplier_id)`.
5. `batch`:
   - `market_id` recebe a demo, com `market_type` gerado `'MARKET'` e FK composta.
   - `status ENUM('PENDING','ACTIVE','REJECTED') NOT NULL`, preenchido com `ACTIVE` (RN-27).
   - `rejection_reason VARCHAR(500) NULL` e `produced_at` passando a NULL.
   - Constraints:
     ```sql
     CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL)),
     CHECK (status <> 'ACTIVE' OR produced_at IS NOT NULL),
     FOREIGN KEY (product_id, supplier_id) REFERENCES product (id, supplier_id)   -- RN-17
     ```
6. `report`: `market_id` recebe a demo, com `market_type` gerado e FK composta.
7. `batch`, `certification` e `report`: trocar `fk_*_supplier` por `(supplier_id, supplier_type) → company(id, type)`, com `supplier_type` gerado (RN-02).

### V9 — `drop_supplier`

- **Views:** recriar as views que leem `supplier`, ou que precisam do mercado, com `CREATE OR REPLACE`:
  - `vw_stage_emission` e `vw_monthly_emission` passam a expor `market_id`.
  - `vw_supplier_sustainability` passa a ler `FROM company WHERE type = 'SUPPLIER'`. O score continua global, porque é do fornecedor.
- **Procedure:** `sp_generate_sustainability_report` ganha `p_market_id` e filtra `vw_stage_emission` por ele (RN-23).
- **Triggers:** apagar as `audit_supplier_*`. Criar `audit_company_*` e `audit_company_plan_*`. Recriar `audit_users_*` (`company_id`), `audit_product_*` (`supplier_id`), `audit_batch_*` (`market_id`, `status`, `rejection_reason`, `produced_at`; `status` sinaliza `STATUS_CHANGE`, como em `certification`) e `audit_report_*` (`market_id`). As colunas geradas `*_type` ficam de fora do snapshot.
- **Grants:** `supply_audit` recebe `SELECT, TRIGGER` em `company` e `company_plan`. `${app_user}` recebe CRUD em `company` e `company_plan` e `SELECT` em `plan`.
- **Remoção:** `DROP TABLE supplier` no fim.

O seed (V2) não muda, porque a V8 converte o que ele gera. `src/test/resources/seed/report-seed.sql` passa a inserir em `company`.

## 2. Domínio e aplicação

| Peça | Mudança |
|---|---|
| `Supplier`, `SupplierRepository`, `SupplierNotFoundException` | Renomeados para `Company*`. `Company` ganha `email` e `type` (`CompanyType` novo) e perde `userId`. A exceção passa a estender `ResourceNotFoundException` (404). |
| `UserRole` | `+ PLATFORM_ADMIN`. Novo método `allowedFor(CompanyType)` com o RN-07: um `switch` e nada mais. |
| `User` | `+ companyId` (nulo só para `PLATFORM_ADMIN`). |
| `Batch` | `+ marketId`, `status` (`BatchStatus` novo) e `rejectionReason`. Métodos `reject(reason)` e `activate(producedAt)` concentram o RN-18 e o RN-19 e lançam `ConflictException` (409) fora de `PENDING`. |
| `Product`, `Report` | `+ supplierId` / `+ marketId`. |
| `PlanRepository` (novo) | `findAll()`, `findByCode(code)`. Sem entidade de assinatura no domínio: `CompanyRepository.subscribe(companyId, planId)` e `activeMaxUsers(companyId)` bastam. |
| `CompanyRepository` | `+ lockById(id)` (`SELECT ... FOR UPDATE`), usado antes de contar admins e usuários, para que dois requests simultâneos não passem juntos pelo RN-04 e pelo RN-08. `+ existsBatchBetween(marketId, supplierId)` para o RN-12. |

### Use cases

| Use case | Regra |
|---|---|
| `RegisterCompanyUseCase` (novo) | Valida CNPJ (`Cnpj` VO existente), email e senha. CNPJ ou email repetido → 409. Plano inexistente → 400. Grava endereço, companhia, assinatura e admin numa transação e devolve o token pelo mesmo caminho do `AuthenticateUserUseCase`, extraído para um método compartilhado. |
| `GetOwnCompanyUseCase` / `UpdateOwnCompanyUseCase` (novos) | Substituem `UpdateOwnSupplierUseCase`. A parte de nome e senha já vive em `OwnAccountChanges`/`PATCH /users/me` (feature 009) e sai daqui. |
| `RegisterUserUseCase` | Usa a companhia do principal (RN-09) e valida `role.allowedFor(type)` (400). Faz `lockById`, conta os usuários e compara com `activeMaxUsers` (409). Os ramos de `supplierId` saem. |
| `UpdateUserRoleUseCase` | O alvo precisa ser da mesma companhia que o autor, exceto para o `PLATFORM_ADMIN` (403). Valida o perfil para o tipo da companhia (400) e recusa `PLATFORM_ADMIN` como destino (400). Com `lockById`, se o alvo é `ADMIN`, o novo perfil não é, e a companhia tem um só `ADMIN`, devolve 409 (RN-08). A trava de mudar de ou para `SUPPLIER` (feature 002) sai, porque o perfil agora pertence à companhia. |
| `RegisterBatchUseCase` | Mercado vem do principal. Produto de outro fornecedor → 400, antes que a FK estoure. Nasce `PENDING`. |
| `RejectBatchUseCase` (novo) | Só o fornecedor do lote; `batch.reject(reason)`. |
| `RegisterChainStageUseCase` | Lote fora do escopo → 403. `REJECTED` → 409. Em `PENDING`, só o fornecedor registra, e só `PRODUCTION`; depois chama `batch.activate(startedAt)`. O usuário do header `X-Responsible-User-Id` precisa ser do mercado ou do fornecedor do lote (403). |
| Transporte, emissão, certificação, relatório | Verificação de escopo pelo lote ou pelo fornecedor (RN-15). O relatório grava `market_id`. |
| Listagens (`ListBatches`, `ListUsers`, `ListAuditLogs`, produtos, certificações, relatórios, dashboard) | Recebem `marketId`/`supplierId`/`companyId` do principal e filtram na query. Nulos para o `PLATFORM_ADMIN`, que vê tudo. O filtro de auditoria é `JOIN user u WHERE u.companyId = :companyId`, sem mudar as triggers (RN-14). |
| Ranking (`RankSuppliersBySustainabilityUseCase`, `SupplierJpaRepository`) | Continua global (RN-13). As queries `findIdsRankedByScore`, `findRankedByEmission` e `countBySearch` recebem `:marketId` e `:workedWithOnly`. `workedWith` é calculado na mesma query com `EXISTS (SELECT 1 FROM batch b WHERE b.supplier_id = c.id AND b.market_id = :marketId)`, e o filtro reaproveita o mesmo `EXISTS`. A contagem de relatórios do item filtra `report.market_id = :marketId`. O filtro `withoutUser` sai. |
| `GetDashboardSummaryUseCase` | Mercado: o card de fornecedores usa `countBySearch("", workedWithOnly = true, marketId)`. Fornecedor: `pendingBatches = countBySupplierIdAndStatus(supplierId, PENDING)`. O DTO ganha `pendingBatches`, nulo para o mercado (RN-13c). |

## 3. Segurança

- `CustomUserPrincipal(userId, email, role, companyId, companyType)` com dois métodos derivados, `supplierId()` e `marketId()`, que devolvem `companyId` quando o tipo bate e `null` caso contrário. Esse é o "escopo" usado em todos os controllers, sem classe nova.
- JWT: os claims passam a ser `companyId` e `companyType`, e `supplierId` continua sendo emitido (RF-04). `AuthenticateUserUseCase` perde a busca `findByUserId`.
- Um bean `RoleHierarchy` com `ROLE_PLATFORM_ADMIN > ROLE_ADMIN` faz o `PLATFORM_ADMIN` herdar todas as regras de `ADMIN` em `SecurityConfig` e nos `@PreAuthorize`. Os use cases que precisam de companhia (criar usuário, `/companies/me`) recusam com 403 quando `companyId` é nulo.
- Diferenças em `SecurityConfig`:

| Rota | Antes | Depois |
|---|---|---|
| `GET /plans`, `POST /companies` | — | `permitAll` |
| `GET/PATCH /companies/me` | — | autenticado / `ADMIN` |
| `POST /users` | `ADMIN`, `MANAGER` | `ADMIN` |
| `GET /users` | `ADMIN`, `MANAGER` | autenticado (filtrado pela companhia, RN-13b) |
| `GET /suppliers/**` | autenticado | autenticado; o controller devolve 403 para companhia `SUPPLIER` fora do próprio detalhe (RN-13a) |
| `POST/PUT /suppliers/**`, `PATCH /suppliers/me` | `ADMIN`, `MANAGER` / `SUPPLIER` | removidas |
| `POST/PUT /products/**` | `ADMIN`, `MANAGER` | `SUPPLIER`, `ADMIN` (o use case exige companhia `SUPPLIER`) |
| `POST /batches` | `SUPPLIER`, `ADMIN` | `MANAGER`, `ADMIN` (o use case exige `MARKET`) |
| `PATCH /batches/*/reject` | — | `SUPPLIER`, `ADMIN` |
| `PATCH /certifications/**` | `AUDITOR`, `ADMIN` | `SUPPLIER`, `ADMIN` |
| `POST /suppliers/*/certifications` | `SUPPLIER`, `ADMIN` | igual, com o dono validado pelo `supplierId()` |

## 4. Web

- `CompanyController` (novo): `POST /companies`, `GET` e `PATCH /companies/me`.
- `PlanController` (novo): `GET /plans`.
- `SupplierController` fica só com as leituras: lista, ranking e detalhe de companhias `SUPPLIER`, além das certificações e dos relatórios aninhados.
- `BatchController`: `PATCH /{id}/reject` e o filtro `status`.
- DTOs novos são `record`s, no padrão atual: `RegisterCompanyRequestDTO`, `CompanyResponseDTO`, `PlanResponseDTO`, `RejectBatchRequestDTO`.
- `LoginResponseDTO` e `UserResponseDTO` ganham os campos do RF-04.
- Swagger com `@Operation` em cada rota nova.

## 5. Testes

**Unitários:**
- `UserRole.allowedFor`.
- `Batch.reject` e `Batch.activate`: transições válidas e 409.
- `RegisterUserUseCase`: limite do plano e perfil inválido para o tipo.
- `UpdateUserRoleUseCase`: último admin, outra companhia e destino `PLATFORM_ADMIN`.
- `RegisterCompanyUseCase`: CNPJ e email duplicados, plano inexistente.

**ITs** (novos, no padrão atual de `@Import(TestcontainersConfiguration.class)`):

| IT | O que cobre |
|---|---|
| `CompanySignupIT` | `POST /companies` devolve um token que funciona em `GET /users/me`. |
| `MigrationToCompanyIT` | Depois da V9: todos os fornecedores viraram `company` com o mesmo id; cada companhia tem uma assinatura ativa e pelo menos um `ADMIN`; existe exatamente um `PLATFORM_ADMIN`; nenhum produto tem lote de outro fornecedor. |
| `CompanyConstraintsIT` | O banco recusa lote com `market_id` de um `SUPPLIER`, segunda assinatura ativa, usuário sem companhia que não é `PLATFORM_ADMIN` e `REJECTED` sem motivo. |
| `TenantIsolationIT` | Dois mercados e um fornecedor com lote só no mercado A: cada mercado só vê o próprio lote, os usuários e a auditoria dele; escrever no lote do outro dá 403; `GET /suppliers` mostra o fornecedor aos dois, com `workedWith` `true` para A e `false` para B, e `workedWith=true` o esconde de B; companhia `SUPPLIER` recebe 403 na lista; um `AUDITOR` lista os usuários da própria companhia e recebe 403 ao criar. |
| `BatchLifecycleIT` | `PENDING` → recusa com motivo → etapa dá 409; `PENDING` → `PRODUCTION` pelo fornecedor → `ACTIVE` com `produced_at`; `PRODUCTION` pelo mercado em `PENDING` dá 403. |
| `UserPlanLimitIT` | O quarto usuário no `BASIC` dá 409; o único admin rebaixando a si mesmo dá 409. |
| `AuditLogTriggerIT` | Ganha casos para `company`, `company_plan` e o `STATUS_CHANGE` de `batch.status`. |

**ITs existentes** que mudam junto:
- `SupplierUserLinkIT` e `SupplierOwnProfileIT` são apagados (o comportamento deles sai).
- `SupplierRecommendationIT` perde os casos de `productId`.
- `BatchListIT`, `ReportDetailIT` e `DashboardEmissionsIT` passam a autenticar como usuário de mercado.
