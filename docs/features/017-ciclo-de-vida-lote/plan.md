# Plano técnico: Ciclo de vida do lote

> Os requisitos estão em [`spec.md`](spec.md) e o passo a passo em [`tasks.md`](tasks.md).

## 1. Máquina de estados no domínio (Task 1)

A máquina de estados é uma tabela no próprio enum, sem biblioteca: são 5 estados e 4 transições. O Spring Statemachine traria eventos, listeners e configuração para resolver o mesmo `switch`.

```java
/** Ciclo de vida do lote (017): PENDING → ACTIVE | REJECTED | CANCELED; ACTIVE → COMPLETED. Os outros são finais. */
public enum BatchStatus {
    PENDING, ACTIVE, COMPLETED, REJECTED, CANCELED;

    public boolean canTransitionTo(BatchStatus next) {
        return switch (this) {
            case PENDING -> next == ACTIVE || next == REJECTED || next == CANCELED;
            case ACTIVE -> next == COMPLETED;
            case COMPLETED, REJECTED, CANCELED -> false;
        };
    }

    public boolean acceptsStages() {
        return this == PENDING || this == ACTIVE;
    }
}
```

O `Batch` passa todas as transições por um único guard:

```java
private void moveTo(BatchStatus next) {
    if (!status.canTransitionTo(next)) throw new BatchStatusConflictException(batchId, status);
    status = next;
}

public void reject(String reason) { /* valida reason */ moveTo(REJECTED); rejectionReason = reason.trim(); }
public void activate(LocalDate producedAt) { /* valida producedAt */ moveTo(ACTIVE); this.producedAt = producedAt; }
public void cancel() { moveTo(CANCELED); }
public void complete() { moveTo(COMPLETED); }
```

O dono da etapa fica no `StageType`, ao lado do `next()` que já existe:

```java
/** RN-08 (017): o mercado registra só o RETAIL; o resto da cadeia é do fornecedor. */
public CompanyType owner() {
    return this == RETAIL ? CompanyType.MARKET : CompanyType.SUPPLIER;
}
```

## 2. Registrar etapa (Task 1)

`RegisterChainStageUseCase.execute` passa a receber o `CompanyType` de quem age, além do `marketId` e do `supplierId`. O `ChainController` passa `principal.companyType()`, que é nulo para o `PLATFORM_ADMIN`.

A nova ordem do use case:

1. Busca o lote (404) e confere `isWithin(marketId, supplierId)` (403), como hoje.
2. `if (!batch.getStatus().acceptsStages()) throw new BatchStatusConflictException(...)` (409). Isso substitui o `if REJECTED`.
3. Calcula `expected` como hoje e confere que `request.stageType() == expected` (`InvalidStageTransitionException`).
4. **Novo:** `if (actorSide != null && actorSide != expected.owner()) throw new AccessDeniedException("A etapa X é registrada pelo <mercado|fornecedor> do lote.")` (RN-08). Isso substitui o `if (pending && supplierId == null)`, porque em `PENDING` a etapa esperada é a `PRODUCTION`, que é do fornecedor. Um guard a menos.
5. Responsável: a companhia dele precisa ser `expected.owner() == SUPPLIER ? batch.getSupplier().getCompanyId() : batch.getMarketId()` (RN-09). O `||` de hoje vira essa igualdade.
6. `if (expected == PRODUCTION) batch.activate(...)` e `if (expected == RETAIL) batch.complete()`. O `save(batch)` acontece quando qualquer um dos dois roda.

Os perfis não precisam de checagem nova. O `SecurityConfig` já restringe a rota a `SUPPLIER`, `MANAGER` e `ADMIN`, e o `UserRole.allowedFor` garante que `MANAGER` só existe em mercado e `SUPPLIER` só em fornecedor. Basta checar o lado.

`SecurityConfig`: `POST /api/v1/stages/*/emission` passa a `hasAnyRole("SUPPLIER", "MANAGER", "ADMIN")` (RN-10). O `CalculateCarbonEmissionUseCase` já limita ao mercado ou ao fornecedor do lote.

## 3. Leituras (Task 1)

- `ProductJpaRepository.findCatalog`: `AND status = 'ACTIVE'` vira `AND status IN ('ACTIVE', 'COMPLETED')` (RN-12).
- `BatchJpaRepository.countActiveBatches`: troca o join com `vw_batch_current_stage` por `select count(*) from batch b where b.status = 'ACTIVE' and ...` (RN-13). A consulta fica mais simples e usa `idx_batch_market_status_created`.
- `BatchController`: o `@Operation` da listagem passa a citar os 5 status, e o `ChainController`, as regras de dono.

## 4. Migration `V20__ciclo_de_vida_lote.sql` (Task 1)

O MySQL não altera uma coluna referenciada por uma check sem antes remover a check. A ordem fica assim:

```sql
-- 1. Enum e checks (RF-01): as checks usam status, então saem antes do MODIFY e voltam depois.
ALTER TABLE batch
    DROP CHECK chk_batch_active_produced_at,
    DROP CHECK chk_batch_rejection_reason;

ALTER TABLE batch
    MODIFY status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'REJECTED', 'CANCELED')
        COLLATE utf8mb4_0900_as_ci NOT NULL;

ALTER TABLE batch
    ADD CONSTRAINT chk_batch_rejection_reason CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL)),
    ADD CONSTRAINT chk_batch_produced_at CHECK (status NOT IN ('ACTIVE', 'COMPLETED') OR produced_at IS NOT NULL);

-- 2. RN-15: lote que já chegou ao varejo está concluído.
UPDATE batch b
SET b.status = 'COMPLETED'
WHERE b.status = 'ACTIVE'
  AND EXISTS (SELECT 1 FROM chain c WHERE c.batch_id = b.id AND c.stage_type = 'RETAIL');

-- 3. RN-16: responsável de etapa do lado errado passa para o primeiro usuário elegível do lado dono.
UPDATE chain c
JOIN batch b ON b.id = c.batch_id
JOIN users u ON u.id = c.responsible_user_id
SET c.responsible_user_id = COALESCE(
        (SELECT MIN(s.id) FROM users s
         WHERE s.company_id = b.supplier_id AND s.is_active AND s.role IN ('ADMIN', 'SUPPLIER')),
        c.responsible_user_id)
WHERE c.stage_type <> 'RETAIL'
  AND NOT (u.company_id <=> b.supplier_id);

UPDATE chain c
JOIN batch b ON b.id = c.batch_id
JOIN users u ON u.id = c.responsible_user_id
SET c.responsible_user_id = COALESCE(
        (SELECT MIN(s.id) FROM users s
         WHERE s.company_id = b.market_id AND s.is_active AND s.role IN ('ADMIN', 'MANAGER')),
        c.responsible_user_id)
WHERE c.stage_type = 'RETAIL'
  AND NOT (u.company_id <=> b.market_id);
```

- A subconsulta lê `users`, não `chain`, então o MySQL aceita o `UPDATE ... JOIN` (não cai no erro 1093).
- O `<=>` trata o `PLATFORM_ADMIN` (`company_id` nulo) como lado errado.
- Os triggers `audit_batch_update` e `audit_chain_update` (V5/V9) registram as linhas alteradas (RN-17). Os nomes exatos dos triggers vigentes precisam ser conferidos na Task 1.
- Antes de commitar, rodar no compose local e conferir:

  ```sql
  SELECT COUNT(*) FROM batch b WHERE b.status = 'ACTIVE'
     AND EXISTS (SELECT 1 FROM chain c WHERE c.batch_id = b.id AND c.stage_type = 'RETAIL');     -- 0
  SELECT COUNT(*) FROM chain c JOIN batch b ON b.id = c.batch_id JOIN users u ON u.id = c.responsible_user_id
   WHERE NOT (u.company_id <=> IF(c.stage_type = 'RETAIL', b.market_id, b.supplier_id));          -- 0, ou só companhias sem usuário elegível
  ```

## 5. Cancelar e recusar (Task 2)

- `CancelBatchUseCase.execute(batchId, companyId, companyType)`: busca o lote (404). Se `companyType != null && (companyType != MARKET || !batch.isWithin(companyId, null))`, devolve 403 com "Só o mercado do lote o cancela.". Depois chama `batch.cancel()` (409 fora de `PENDING`) e salva.
- `RejectBatchUseCase` ganha a mesma assinatura: `companyType` nulo (o `PLATFORM_ADMIN`) passa, e de resto só o `SUPPLIER` dono passa (RF-04).
- `BatchController`: `@PatchMapping("/batches/{batchId}/cancel")`, sem corpo, passando `principal.companyId()` e `principal.companyType()`.
- `SecurityConfig`: `PATCH /api/v1/batches/*/cancel` com `hasAnyRole("MANAGER", "ADMIN")`.

`ponytail:` não criamos um `BatchTransitionUseCase` genérico. Os dois use cases têm 10 linhas cada. Vale juntá-los se aparecer uma terceira transição por endpoint.

## 6. Testes

- `BatchStatusTest` (unitário): a tabela completa, com 5×5 pares. Só os 4 pares válidos devolvem `true`.
- `BatchTest`: `cancel` e `complete` fora do status de origem lançam `BatchStatusConflictException`.
- `RegisterChainStageUseCaseTest`:
  - Mercado registrando `STORAGE` recebe 403. Fornecedor registrando `RETAIL` recebe 403.
  - Mercado registrando `RETAIL` deixa o lote `COMPLETED`.
  - Um lote `COMPLETED` recebe 409.
  - Responsável do mercado numa etapa do fornecedor recebe 403.
  - O `PLATFORM_ADMIN` registra a etapa do lado certo, com responsável elegível.
- `BatchLifecycleIT` (Testcontainers):
  - Fluxo completo: pedir → `PRODUCTION` (fornecedor) → … → `RETAIL` (mercado) termina em `COMPLETED`.
  - Pedir → cancelar termina em `CANCELED`. Cancelar de novo devolve 409. Fornecedor cancelando devolve 403.
  - O `SUPPLIER` calcula a emissão (200).
- `MigrationLifecycleIT`: depois da `V20`, as duas consultas da §4 devolvem 0 no seed, e `findCatalog` conta os lotes `COMPLETED`.
