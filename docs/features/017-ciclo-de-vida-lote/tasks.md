# Tarefas: Ciclo de vida do lote

> Os requisitos estão em [`spec.md`](spec.md) e a arquitetura em [`plan.md`](plan.md).

**Status:** planejada.

Cada task mexe em **no máximo 10 arquivos** (os testes contam) e deixa o `./mvnw verify` verde. As dependências "web Task N" apontam para `supply-chain-verde-web/docs/features/016-ciclo-de-vida-lote/tasks.md`.

Ordem entre os repositórios: **web Task 1 → API Task 1 → web Task 2 → API Task 2 → web Task 3**.

A web Task 1 vem primeiro porque o front valida o `status` com `z.enum`. Se a API mandar `COMPLETED` antes do front conhecer esse valor, a listagem de lotes quebra.

---

## Task 1: `feat/maquina-estados-lote`

Depende da web Task 1 em produção.

- [x] `V20__ciclo_de_vida_lote.sql`: enum, checks, `COMPLETED` dos legados e reatribuição dos responsáveis (plan §4). Conferir as duas consultas no compose antes de commitar
- [x] `BatchStatus`: `COMPLETED`, `CANCELED`, `canTransitionTo` e `acceptsStages` (plan §1)
- [x] `Batch`: `moveTo`, `cancel` e `complete`, e `reject`/`activate` passando pelo `moveTo`
- [x] `StageType.owner()`
- [x] `RegisterChainStageUseCase` e `ChainController`: o lado de quem age, o dono da etapa, o responsável do lado dono e o `complete()` no `RETAIL` (plan §2)
- [x] `SecurityConfig`: `SUPPLIER` no `POST /stages/*/emission`
- [x] `ProductJpaRepository.findCatalog` (`ACTIVE` ou `COMPLETED`) e `BatchJpaRepository.countActiveBatches` (`status = 'ACTIVE'`) (plan §3)
- [x] Testes: `BatchStatusTest`, `BatchTest`, `RegisterChainStageUseCaseTest` e `MigrationLifecycleIT`

Se passar de 10 arquivos, a `V20` e o `MigrationLifecycleIT` vão num commit próprio da mesma branch.

**Arquivos (~10):** `V20`, `BatchStatus`, `Batch`, `StageType`, `RegisterChainStageUseCase`, `ChainController`, `SecurityConfig`, `ProductJpaRepository`/`BatchJpaRepository`, testes.

**Pronto:** `./mvnw verify` verde. No compose local, o mercado recebe 403 ao registrar `STORAGE`, e registrar `RETAIL` deixa o lote `COMPLETED`.

## Task 2: `feat/cancelar-lote`

Depende da Task 1.

- [x] `CancelBatchUseCase` (plan §5)
- [x] `RejectBatchUseCase`: passa a receber `companyId`/`companyType` e deixa o `PLATFORM_ADMIN` passar (RF-04)
- [x] `BatchController`: `PATCH /batches/{id}/cancel`, a nova chamada do reject e o `@Operation` com os 5 status
- [x] `SecurityConfig`: `PATCH /api/v1/batches/*/cancel` para `MANAGER` e `ADMIN`
- [x] Testes: `CancelBatchUseCaseTest`, `RejectBatchUseCaseTest` e `BatchLifecycleIT` (plan §6)

**Arquivos (~7):** `CancelBatchUseCase`, `RejectBatchUseCase`, `BatchController`, `SecurityConfig`, testes.

**Pronto:** `./mvnw verify` verde. No compose local, um `MANAGER` cancela um lote pendente, e o fornecedor recebe 403 na mesma chamada.
