# Tarefas — Empresas, planos e isolamento por companhia

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** em andamento (Tasks 1 a 3 concluídas).

As tasks são sequenciais, e cada uma deixa o `./mvnw verify` verde. As tasks do frontend (`supply-chain-verde-web/docs/features/009-empresas-planos/tasks.md`) dependem destas, conforme indicado nelas.

---

## Task 1 — `feat/company-schema`

Só banco, com o mínimo de Java para a aplicação continuar subindo.

- [x] V7, V8 e V9 (plan §1)
- [x] Entidades e repositórios JPA de `Supplier` apontando para `company`, filtrando `type = 'SUPPLIER'`, sem renomear nada ainda
- [x] `src/test/resources/seed/report-seed.sql` no esquema novo
- [x] `MigrationToCompanyIT`, `CompanyConstraintsIT` e os casos novos de `AuditLogTriggerIT` (plan §5)

**Pronto:** o banco migra do zero e a partir da V6; os ITs atuais continuam verdes, exceto os que dependem de `supplier.user_id`, que são marcados para a Task 3.

## Task 2 — `feat/company-signup`

- [x] Renomear `Supplier*` para `Company*` no domínio, na aplicação e na persistência (plan §2)
- [x] `PlanRepository`, `GET /plans` e `POST /companies` (`RegisterCompanyUseCase`)
- [x] `GET` e `PATCH /companies/me`; remover `PATCH /suppliers/me`, `POST /suppliers` e `PUT /suppliers/{id}`
- [x] `CompanySignupIT` e testes unitários do `RegisterCompanyUseCase`

**Pronto:** uma empresa se cadastra e já usa a API logada. Isso libera a Task 1 do frontend.

## Task 3 — `feat/company-scope`

- [x] `PLATFORM_ADMIN`, `RoleHierarchy`, o novo `CustomUserPrincipal` e os claims do JWT (plan §3)
- [x] `GET /users/me` e o login com `companyId`, `companyType` e `companyName`
- [x] Regras de usuário: perfil por tipo, limite do plano, último admin e lock (plan §2)
- [x] Filtro por companhia em usuários (leitura para todos), auditoria, produtos, certificações, relatórios e dashboard
- [x] Catálogo de fornecedores com `workedWith` e o filtro, 403 para companhia fornecedora, e `pendingBatches` no dashboard (plan §2)
- [x] `TenantIsolationIT` e `UserPlanLimitIT`; apagar `SupplierUserLinkIT` e `SupplierOwnProfileIT`

**Pronto:** dois mercados não enxergam nada um do outro, e o `PLATFORM_ADMIN` vê tudo. Isso libera as Tasks 2 e 3 do frontend.

## Task 4 — `feat/batch-request`

- [ ] `POST /batches` pelo mercado, sem `producedAt`, validando o produto contra o fornecedor
- [ ] Produtos pelo fornecedor (`POST/PUT /products`, `GET /products?supplierId=`)
- [ ] `PATCH /batches/{id}/reject`, filtro `status` e transição para `ACTIVE` na primeira etapa
- [ ] Escopo em etapa, transporte, emissão e `X-Responsible-User-Id`
- [ ] Certificação gerenciada pelo fornecedor; relatório com `market_id`
- [ ] Recomendação só por `category` + `unit`
- [ ] `BatchLifecycleIT`; ajustar `SupplierRecommendationIT`, `BatchListIT`, `ReportDetailIT` e `DashboardEmissionsIT`

**Pronto:** o mercado pede o lote, e o fornecedor executa ou recusa. Isso libera a Task 4 do frontend.

## Task 5 — `docs/company-reference`

- [ ] Rotas novas e removidas em `docs/features/000-base/reference.md`
- [ ] README: perfis, multiempresa e diagrama de entidades atualizados
- [ ] ADR 0003 curto sobre a FK composta por tipo de companhia e a assinatura ativa única

**Pronto:** documentação bate com a API.
