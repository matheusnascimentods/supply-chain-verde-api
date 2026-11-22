# Tarefas — Gestão de produtos no pedido de lote

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** não iniciada.

Depende da `014-busca-full-text` concluída. Cada task mexe em **no máximo 10 arquivos** (testes contam) e deixa o `./mvnw verify` verde. A dependência "web Task N" aponta para `supply-chain-verde-web/docs/features/014-gestao-produtos/tasks.md`.

Ordem entre os repositórios: **API Task 1 → web Task 1 → web Task 2 → web Task 3 → API Task 2 → API Task 3**.

---

## Task 1 — `feat/catalogo-pedido-lote`

- [ ] `V15__idx_batch_market_status_created.sql` (plan §2)
- [ ] `ProductRepository`: a projeção `CatalogItem`, mais `findCatalog`/`countCatalog` (plan §3)
- [ ] `ProductJpaRepository`: as native queries do catálogo (plan §2)
- [ ] `ProductRepositoryImpl`: `booleanQuery` e conversão dos enums
- [ ] `ProductResponseDTO` com `supplierName`, `co2KgPerUnit` e `acceptedBatchesLast30Days`, e o `ProductDtoMapper.toResponse(CatalogItem)`
- [ ] `ListProductsUseCase`: ramo do mercado no catálogo, ramo do fornecedor sem mudança
- [ ] `ProductController`: `category`, `unit` e `principal.marketId()`, mais o texto do `@Operation`
- [ ] `ProductControllerIT`: os casos do plan §5

**Arquivos (9):** `V15__idx_batch_market_status_created.sql`, `ProductRepository`, `ProductJpaRepository`, `ProductRepositoryImpl`, `ProductResponseDTO`, `ProductDtoMapper`, `ListProductsUseCase`, `ProductController`, `ProductControllerIT`.

**Pronto:** `./mvnw verify` verde. No compose local, `GET /products` como `h.kane@bayern.com` traz os campos novos, ordenados pela RN-02.

## Task 2 — `refactor/remove-recomendacao-categoria`

Depende da web Task 3 em produção: o front não chama mais `ranked=true&category=...&unit=...`.

- [ ] `grep` de confirmação (plan §4)
- [ ] `SupplierController`: saem `category`/`unit` e a validação do par
- [ ] `RankSuppliersBySustainabilityUseCase`: sai o ramo da recomendação
- [ ] `CompanyRepository`, `CompanyRepositoryImpl` e `CompanyJpaRepository`: saem `findRankedByEmission` e `SupplierEmissionRank`
- [ ] `SupplierRecommendationIT`: apagar, conferindo se os casos de ordem estão no `ProductControllerIT`

**Arquivos (~6):** `SupplierController`, `RankSuppliersBySustainabilityUseCase`, `CompanyRepository`, `CompanyRepositoryImpl`, `CompanyJpaRepository`, `SupplierRecommendationIT`.

**Pronto:** `./mvnw verify` verde, e o grep do plan §4 não acha nada.

## Task 3 — `docs/gestao-produtos`

Depende das Tasks 1 e 2.

- [ ] `docs/features/000-base/reference.md`: `GET /products` com `category`/`unit` e os campos novos, sem a recomendação em `/suppliers`, e a nota RN-21 → RN-02 desta feature
- [ ] `docs/features/010-empresas-planos/spec.md`: RN-21 marcada como substituída por esta feature

**Arquivos (~2):** `reference.md`, `010-empresas-planos/spec.md`.

**Pronto:** a documentação bate com a API.
