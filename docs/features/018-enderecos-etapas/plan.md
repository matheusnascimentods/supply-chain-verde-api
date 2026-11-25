# Plano técnico: Endereços das etapas do lote

> Os requisitos estão em [`spec.md`](spec.md) e o passo a passo em [`tasks.md`](tasks.md).

## 1. Origem derivada no domínio (Task 1)

A regra fica num método estático do `Chain`, sem um serviço novo. Os três pontos que montam `ChainResponseDTO` já têm em mãos as etapas do lote.

```java
/** RN-04 (018): a origem da PRODUCTION é o fornecedor; a das outras etapas é o destino da etapa anterior. */
public static Map<Long, Address> origins(List<Chain> stagesOfOneBatch) {
    Map<Long, Address> origins = new HashMap<>();
    Address previous = null;
    for (Chain stage : stagesOfOneBatch.stream().sorted(Comparator.comparing(Chain::getStageType)).toList()) {
        origins.put(stage.getChainId(), previous != null ? previous : stage.getBatch().getSupplier().getAddress());
        previous = stage.getDestinationAddress();
    }
    return origins;
}
```

- A ordenação usa o `stage_type`, não o `startedAt`. A `007-ordem-etapas` garante que a sequência do enum é a ordem real, enquanto `startedAt` pode vir fora de ordem.
- Antes da `V21` ainda pode haver destino nulo. Nesse caso a origem da etapa seguinte fica nula, e o front já trata `null`. Depois do backfill, isso não acontece mais.
- O `HashMap` aceita valor `null`, e o `Collectors.toMap` não aceitaria. Por isso não se usa stream nesse ponto.

`ChainDtoMapper.toResponse` ganha o parâmetro `Address origin`, mapeado para `originAddress`. Os chamadores:

| Chamador | Como obtém as etapas |
|---|---|
| `BatchStagesReader.findByBatchIds` | Já carrega todas as etapas dos lotes. Basta agrupar por `batchId` e chamar `Chain.origins` por grupo. |
| `ListChainStagesByBatchUseCase` | `findByBatchId` devolve as etapas do lote. Chama `Chain.origins` uma vez. |
| `RegisterChainStageUseCase` | Já chama `findByBatchId` para descobrir a etapa esperada. A origem da etapa nova é o destino da maior etapa existente, ou o endereço do fornecedor se não houver nenhuma. |

O `ChainJpaRepository` já faz fetch de `batch.supplier.address` (`@EntityGraph`), então isso não gera N+1.

## 2. Registrar etapa (Task 1)

`ChainRequestDTO`:

```java
public record ChainRequestDTO(Long batchId, AddressRequestDTO destination, StageType stageType,
                              LocalDateTime startedAt, LocalDateTime endedAt) {}
```

Em `RegisterChainStageUseCase`, depois das validações de status, sequência, dono e responsável da 017, e antes do `activate`/`complete`:

```java
Address destination = expected == StageType.RETAIL
        ? marketAddress(batch, request)        // RN-03
        : addressRepository.save(validDestination(request.destination()));  // RN-01, RN-02
```

- `marketAddress`: se `request.destination() != null`, lança `ValidationException("O destino do varejo é o endereço do mercado e não pode ser informado.")`. Senão, devolve `companyRepository.findById(batch.getMarketId()).orElseThrow().getAddress()`. O use case ganha `CompanyRepository` e perde o `findById` de endereço.
- `validDestination`: se for nulo, lança `ValidationException("Destino da etapa é obrigatório.")`. Se algum campo obrigatório estiver em branco, lança "Destino incompleto…". Depois aplica `trim` e `addressMapper.toDomain`. O `state` passa por `toUpperCase`.
- `.originAddress(...)` sai do builder.
- O `@Operation` do `ChainController` documenta `destination` e as RN-01 e RN-03.

`ponytail:` a validação de campo em branco é um `if` com `isBlank` no próprio use case, sem Bean Validation. O resto da API também valida assim (`RegisterCompanyUseCase`). Vale extrair para um `Address.requireComplete()` quando a terceira tela usar a mesma regra.

## 3. Migration `V21__enderecos_etapas.sql` (Task 2)

```sql
-- 1. RN-07: destino nulo = ficou no lugar (origem antiga) ou, sem ela, o fornecedor.
UPDATE chain c
JOIN batch b ON b.id = c.batch_id
JOIN company s ON s.id = b.supplier_id
SET c.destination_address_id = COALESCE(c.origin_address_id, s.address_id)
WHERE c.destination_address_id IS NULL AND c.stage_type <> 'RETAIL';

-- 2. RN-08: o varejo termina no mercado.
UPDATE chain c
JOIN batch b ON b.id = c.batch_id
JOIN company m ON m.id = b.market_id
SET c.destination_address_id = m.address_id
WHERE c.stage_type = 'RETAIL';

-- 3. RN-09: triggers antes da coluna (o corpo deles cita origin_address_id).
DROP TRIGGER audit_chain_insert;
DROP TRIGGER audit_chain_update;
DROP TRIGGER audit_chain_delete;

ALTER TABLE chain
    DROP FOREIGN KEY fk_chain_origin_address,
    DROP INDEX idx_chain_origin_address_id,
    DROP COLUMN origin_address_id,
    MODIFY destination_address_id BIGINT NOT NULL;

-- 4. Recria os 3 triggers copiando o corpo vigente da V5, sem as linhas de origin_address_id.
```

- `ponytail:` o backfill da RN-07 não percorre a cadeia. Se a origem antiga também for nula, o destino vira o endereço do fornecedor, mesmo que a etapa anterior tenha terminado em outro lugar. Só afeta dados criados pela UI antiga com "Não informar". Se isso aparecer em dados reais, a melhoria é um backfill em ordem de `stage_type`.
- Os triggers são `DEFINER = 'supply_audit'@'%'`. A V5 mostra como a migration os cria, e a V21 segue o mesmo padrão. Antes de copiar, conferir se alguma migration posterior à V5 recriou esses triggers (`grep audit_chain` em `db/migration`).
- Conferir no compose local antes de commitar:

  ```sql
  SELECT COUNT(*) FROM chain WHERE destination_address_id IS NULL;                          -- 0
  SELECT COUNT(*) FROM chain c JOIN batch b ON b.id = c.batch_id JOIN company m ON m.id = b.market_id
   WHERE c.stage_type = 'RETAIL' AND c.destination_address_id <> m.address_id;               -- 0
  ```

## 4. Limpeza da persistência (Task 2)

- `Chain`: sai o campo `originAddress`.
- `ChainJpaEntity`: sai `originAddress`, e `destinationAddress` passa a `optional = false`.
- `ChainJpaRepository`, `TransportJpaRepository` e `CarbonEmissionJpaRepository`: sai `chain.originAddress` / `originAddress` dos `@EntityGraph`. No transporte e na emissão, entra `chain.batch.supplier.address` onde a origem for lida. Hoje esses grafos alimentam relatórios que não leem a origem, e isso precisa ser confirmado com um grep antes de mexer.
- `ChainJpaMapper`: sai o mapeamento da origem. Também conferir `TransportJpaMapper` e `CarbonEmissionJpaMapper`, que mapeiam o `chain` aninhado.
- Os seeds de teste (`src/test/resources/seed/*.sql`) não citam `origin_address_id` e não precisam mudar. A `V2` cita, mas já foi aplicada e o Flyway não a reexecuta.

## 5. Testes

- `ChainTest` (unitário): `origins` com etapas fora de ordem, a primeira apontando para o fornecedor e destino nulo no meio.
- `RegisterChainStageUseCaseTest`:
  - `PRODUCTION` sem destino recebe 400. Destino com `city` em branco recebe 400.
  - `PRODUCTION` válida chama `addressRepository.save`, e a resposta traz a origem igual ao endereço do fornecedor.
  - `RETAIL` com destino recebe 400. `RETAIL` sem destino usa o endereço do mercado e não chama `save`.
- `BatchStagesReader`/`ListChainStagesByBatchUseCase`: a origem de cada etapa é o destino da anterior.
- `StageAddressIT` (Testcontainers): cadeia completa PRODUCTION → … → RETAIL pela API, conferindo a rota em `GET /batches/{id}/stages`.
- `MigrationStageAddressIT`: depois da `V21`, as duas consultas da §3 devolvem 0 e um `UPDATE` em `chain` gera linha de auditoria.
