# Tarefas: Cálculo de carbono

> Os requisitos estão em [`spec.md`](spec.md) e a arquitetura em [`plan.md`](plan.md).

**Status:** planejada.

Cada task mexe em **no máximo 10 arquivos** (os testes contam) e deixa o `./mvnw verify` verde. As dependências "web Task N" apontam para `supply-chain-verde-web/docs/features/018-calculo-de-carbono/tasks.md`.

Ordem entre os repositórios: **API Task 1 → API Task 2 → API Task 3 → web Task 1 → API Task 4 → web Task 2 → web Task 3 → API Task 5 → API Task 6**.

- A Task 4 (cálculo por atividade) vem antes da web Task 2 para que nenhuma emissão nova seja gravada com o fator fixo de 0,15. No intervalo entre as duas, o front atual não manda o dado da atividade e recebe 400 nas etapas fixas. Por isso a web Task 2 deve ir para produção logo em seguida.
- A Task 5 (bloqueio) espera a web Task 3 (emissão pendente). Sem ela, um lote com etapa sem emissão travaria sem saída na interface.

---

## Task 1: `fix/emissao-duplicada`

Não depende de nada.

- [x] `CalculateCarbonEmissionUseCase` (+ teste): 409 na segunda emissão (plan §1)
- [x] `GetBatchCarbonFootprintUseCase` (+ teste): usar o `CarbonFootprintCalculator`
- [ ] `supply-chain-verde.md` (fora do repositório): tirar o `SustainabilityScoreCalculator`

**Arquivos (~4):** `CalculateCarbonEmissionUseCase`, `GetBatchCarbonFootprintUseCase`, testes.

**Pronto:** `./mvnw verify` verde. No compose, a segunda chamada de `POST /stages/{id}/emission` devolve 409.

## Task 2: `feat/tabela-de-fatores`

Não depende de nada. Precisa ir para produção antes da web Task 1.

- [ ] `V22__fatores_de_emissao.sql`: a tabela e os 18 fatores, o `GRANT SELECT`, o legado para `GHG_PROTOCOL`, o enum novo, as colunas `cargo_weight_kg`, `energy_source` e `consumption`, o peso dos transportes antigos e as triggers de auditoria recriadas (plan §2). Conferir as consultas no compose antes de commitar
- [ ] `CalculationMethod` sem `IPCC` e `EMEP_EEA`, e o enum novo `EnergySource`
- [ ] `CarbonEmission` e `CarbonEmissionJpaEntity`: os três campos novos
- [ ] Testes que usam `IPCC` ou `EMEP_EEA`: `ViewsAndProceduresIT`, `ApplicationUseCasesTest`, `ProductControllerIT` e `BatchLifecycleIT`
- [ ] As asserções de migration do plan §7 (`V22`) no `ViewsAndProceduresIT`

Se passar de 10 arquivos, a `V22` e as asserções de migration vão num commit próprio da mesma branch.

**Arquivos (~10):** `V22`, `CalculationMethod`, `EnergySource`, `CarbonEmission`, `CarbonEmissionJpaEntity`, 4 testes.

**Pronto:** `./mvnw verify` verde. No compose, `SELECT calculation_method, COUNT(*) FROM carbon_emission GROUP BY 1` só mostra `DEFRA` e `GHG_PROTOCOL`, e a soma de `co2_kg` é a mesma de antes da migration.

## Task 3: `feat/consulta-de-fatores`

Depende da Task 2.

- [ ] `EmissionFactorRepository` (domínio), `EmissionFactorJpaEntity`, `EmissionFactorJpaRepository` e `EmissionFactorRepositoryImpl` (plan §3)
- [ ] `ListEmissionFactorsUseCase` e `EmissionFactorResponseDTO`
- [ ] `CarbonEmissionController`: `GET /emission-factors`, com o `@Operation`
- [ ] Testes: `EmissionFactorsIT`

**Arquivos (~8):** os 4 de persistência, o use case, o DTO, o controller e o IT.

**Pronto:** `./mvnw verify` verde. No compose, `GET /emission-factors` devolve as 18 linhas, e sem token devolve 401.

## Task 4: `feat/calculo-por-atividade`

Depende da Task 3 e da web Task 1 em produção.

- [ ] `StageType.isTransport()`
- [ ] `CarbonEmissionRequestDTO`, `CarbonEmissionResponseDTO` e `CarbonEmissionDtoMapper`: o dado da atividade
- [ ] `CalculateCarbonEmissionUseCase` (+ teste): fator da tabela, peso derivado ou informado e consumo (plan §4)
- [ ] `RegisterTransportUseCase` (+ teste): transporte só em etapa de deslocamento
- [ ] `CarbonEmissionController`: o `@Operation` com os dois formatos de corpo

**Arquivos (~9):** `StageType`, os 2 DTOs, o mapper, os 2 use cases, o controller e 2 testes.

**Pronto:** `./mvnw verify` verde. No compose, os quatro primeiros critérios de aceite da spec passam.

## Task 5: `feat/emissao-obrigatoria`

Depende da Task 4 e da web Task 3 em produção.

- [ ] `RegisterChainStageUseCase` (+ teste): 409 com a etapa anterior sem emissão (plan §5)
- [ ] Os ITs que registram etapas seguidas: o helper `registerStageWithEmission`

**Arquivos (~6):** o use case, o teste e os ITs apontados pelo `./mvnw verify`.

**Pronto:** `./mvnw verify` verde. No compose, registrar a `STORAGE` sem a emissão da `PRODUCTION` devolve 409, e depois do cálculo devolve 201.

## Task 6: `feat/score-por-intensidade`

Depende da Task 4. Faz mais sentido depois de algumas semanas de emissões novas com peso.

- [ ] `V23__score_por_intensidade.sql`, com o `K` calibrado no seed (plan §6)
- [ ] Teste: os dois fornecedores na ordem da intensidade (plan §7)
- [ ] `000-base/reference.md` e `supply-chain-verde.md`: o score novo
- [ ] `debitos.md`: marcar D-1 a D-12 como resolvidos, ou dizer onde cada um ficou fora do escopo

**Arquivos (~4):** `V23`, o teste, o `reference.md` e o `debitos.md`.

**Pronto:** `./mvnw verify` verde. No compose, `GET /suppliers?ranked=true` ordena pela intensidade.
