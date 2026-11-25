# Tarefas: Endereços das etapas do lote

> Os requisitos estão em [`spec.md`](spec.md) e a arquitetura em [`plan.md`](plan.md).

**Status:** implementada.

Cada task mexe em **no máximo 10 arquivos** (os testes contam) e deixa o `./mvnw verify` verde. As dependências "web Task N" apontam para `supply-chain-verde-web/docs/features/017-enderecos-etapas/tasks.md`.

Ordem entre os repositórios: **API Task 1 + web Task 1 (deploy junto) → API Task 2**.

O contrato do `POST /stages` muda de forma incompatível (`destination` substitui os ids). Como o projeto é acadêmico e não mantém compatibilidade, as duas Tasks 1 sobem no mesmo deploy, sem versão de transição.

---

## Task 1: `feat/destino-etapa`

Depende de nada. Sobe junto com a web Task 1.

- [x] `Chain.origins` (plan §1)
- [x] `ChainDtoMapper.toResponse` com o parâmetro `origin`. Ajustar `BatchStagesReader` e `ListChainStagesByBatchUseCase`
- [x] `ChainRequestDTO` com `destination` (RF-01)
- [x] `RegisterChainStageUseCase`: destino obrigatório e salvo, `RETAIL` com o endereço do mercado, origem derivada no response e sem gravar origem (plan §2)
- [x] `ChainController`: `@Operation` com `destination` e as regras
- [x] Testes: `RegisterChainStageUseCaseTest`, `BatchStagesReaderTest` (cobre o `Chain.origins` fora de ordem) e dois casos novos no `BatchLifecycleIT` (origem e destino pela API, 400 sem destino), em vez de um `ChainTest` e um `StageAddressIT` separados. Os ITs que postam etapa (`BatchLifecycleIT`, `TenantIsolationIT` e outros) passam a enviar `destination`

Se passar de 10 arquivos, o ajuste dos ITs existentes vai num commit próprio da mesma branch.

**Arquivos (~10):** `Chain`, `ChainDtoMapper`, `BatchStagesReader`, `ListChainStagesByBatchUseCase`, `ChainRequestDTO`, `RegisterChainStageUseCase`, `ChainController`, testes.

**Pronto:** `./mvnw verify` verde. No compose local, a rota de um lote novo vai do fornecedor, passa pelos destinos digitados e termina no mercado.

## Task 2: `refactor/remove-origem-etapa`

Depende da Task 1 em produção, porque a partir dela nenhuma etapa nova grava a origem.

- [x] `V21__enderecos_etapas.sql`: backfill, `NOT NULL`, remoção da coluna e recriação dos triggers (plan §3). Conferir as duas consultas no compose antes de commitar
- [x] `Chain`, `ChainJpaEntity` e `ChainJpaMapper` sem `originAddress` (plan §4)
- [x] `@EntityGraph` de `ChainJpaRepository`, `TransportJpaRepository` e `CarbonEmissionJpaRepository`
- [x] Teste: `MigrationStageAddressIT`

**Arquivos (10):** `V21`, `Chain`, `ChainJpaEntity`, 3 repositórios JPA, `MigrationStageAddressIT` e os ITs que inserem `chain` direto no SQL (`StageOrderIT`, `ProductControllerIT`, `ViewsAndProceduresIT`), que passam a gravar o destino por causa do `NOT NULL`. O `ChainJpaMapper` não precisou mudar, porque o MapStruct mapeia por nome.

**Pronto:** `./mvnw verify` verde. `SHOW COLUMNS FROM chain` não lista `origin_address_id`, e registrar uma etapa gera auditoria.
