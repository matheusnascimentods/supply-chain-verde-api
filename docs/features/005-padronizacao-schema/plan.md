# Plano técnico — Padronização do schema

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Migrations

| Migration | Conteúdo |
|---|---|
| `V34__replace_search_columns_with_expression_indexes.sql` | Colunas `tsvector`/trigram → índices de expressão (RN-06) |
| `V35__standardize_ids_and_timestamps.sql` | PK `id`, `created_at`/`updated_at`, triggers, histórico de auditoria, views |

As `V1`–`V33` não mudam: o Flyway valida o checksum delas, e os seeds rodam antes da `V35`, com os nomes antigos.

### 1.1 `V34`

```sql
ALTER TABLE supplier DROP COLUMN name_search, DROP COLUMN cnpj_digits;
ALTER TABLE product  DROP COLUMN search_vector;

CREATE INDEX idx_supplier_name_fts ON supplier
    USING GIN (to_tsvector('portuguese'::regconfig, coalesce(name, '')));
CREATE INDEX idx_supplier_cnpj_digits_trgm ON supplier
    USING GIN (regexp_replace(cnpj::text, '[^0-9]', '', 'g') gin_trgm_ops);
CREATE INDEX idx_product_fts ON product
    USING GIN (to_tsvector('portuguese'::regconfig, coalesce(name, '') || ' ' || coalesce(description, '')));
```

Os índices `idx_supplier_name_search`, `idx_supplier_cnpj_digits_trgm` (o antigo) e `idx_product_search_vector` caem junto com as colunas.

O `excluded_columns` do `capture_audit_log` (`V30`) continua listando `search_vector` e `name_search`. Fica assim, porque é inofensivo e reescrever a função só para tirar duas strings não compensa.

O planner só usa o índice se a query repetir a expressão do índice **literalmente**, inclusive o `::regconfig`.

### 1.2 `V35`

Ordem dentro da migration (uma transação só):

1. **Derruba as views** (`vw_monthly_emission`, `vw_supplier_product_emission`, `vw_stage_emission`). Elas bloqueiam a troca de tipo de `calculated_at`.
2. **Derruba os triggers `audit_*`**. Isso evita gravar no `audit_log` uma linha por registro durante o backfill, e os triggers precisam ser recriados de qualquer forma, porque o argumento da PK muda.
3. **Renomeia as PKs**: `ALTER TABLE <t> RENAME COLUMN <t>_id TO id` em `address`, `supplier`, `certification`, `product`, `batch`, `users` (`user_id`), `chain`, `transport`, `carbon_emission` (`emission_id`), `report` e `audit_log` (`log_id`). FKs, constraints e sequências acompanham sozinhas, porque o Postgres referencia colunas pelo número.
4. **Criação**:

   ```sql
   -- já existiam: renomeia e converte (valor antigo interpretado em São Paulo)
   ALTER TABLE supplier        RENAME COLUMN registered_at TO created_at;
   ALTER TABLE report          RENAME COLUMN generated_at  TO created_at;
   ALTER TABLE audit_log       RENAME COLUMN performed_at  TO created_at;
   ALTER TABLE carbon_emission RENAME COLUMN calculated_at TO created_at;

   ALTER TABLE <t> ALTER COLUMN created_at TYPE TIMESTAMPTZ
       USING created_at::timestamp AT TIME ZONE 'America/Sao_Paulo',
       ALTER COLUMN created_at SET DEFAULT now();   -- users, supplier, report, audit_log, carbon_emission

   -- não existiam: linhas antigas ficam com o instante da migration
   ALTER TABLE <t> ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
       -- address, certification, product, batch, chain, transport
   ```

   As colunas que eram `DATE` (`registered_at` e `calculated_at`) viram meia-noite em São Paulo. Assim, o dia exibido pela API continua o mesmo.
5. **`updated_at`** em todas as tabelas, menos `audit_log`: `ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now()` seguido de `UPDATE <t> SET updated_at = created_at`. O `UPDATE` não gera auditoria, porque os triggers estão derrubados (passo 2).
6. **Trigger `set_updated_at`**:

   ```sql
   CREATE FUNCTION set_updated_at() RETURNS TRIGGER LANGUAGE plpgsql AS $$
   BEGIN
       IF NEW IS DISTINCT FROM OLD THEN
           NEW.updated_at := now();
       END IF;
       RETURN NEW;
   END;
   $$;

   CREATE TRIGGER set_updated_at_<t> BEFORE UPDATE ON <t>
   FOR EACH ROW EXECUTE FUNCTION set_updated_at();   -- 10 tabelas
   ```

   Um `UPDATE` que não muda nada também não muda `updated_at`. O `capture_audit_log` já ignora esse caso (`has_changes`), então ele continua sem gerar linha de auditoria.
7. **Recria os triggers `audit_*`** iguais aos da `V29`, trocando o 1º argumento por `'id'`. Exemplo: `capture_audit_log('id', 'status')` em `certification`. A função não muda.
8. **Reescreve o histórico de auditoria (RN-07)**. Só a chave da PK da própria tabela muda, então as FKs dentro do snapshot (`supplier_id` num `batch`) ficam como estão:

   ```sql
   DO $$
   DECLARE m TEXT[];
   BEGIN
       FOREACH m SLICE 1 IN ARRAY ARRAY[
           ['address','address_id','id'], ['users','user_id','id'], ['supplier','supplier_id','id'],
           ['certification','certification_id','id'], ['product','product_id','id'], ['batch','batch_id','id'],
           ['chain','chain_id','id'], ['transport','transport_id','id'], ['carbon_emission','emission_id','id'],
           ['report','report_id','id'], ['supplier','registered_at','created_at'],
           ['report','generated_at','created_at'], ['carbon_emission','calculated_at','created_at']
       ] LOOP
           UPDATE audit_log
           SET before_data = CASE WHEN before_data ? m[2]
                   THEN (before_data - m[2]) || jsonb_build_object(m[3], before_data -> m[2]) ELSE before_data END,
               after_data  = CASE WHEN after_data ? m[2]
                   THEN (after_data - m[2]) || jsonb_build_object(m[3], after_data -> m[2]) ELSE after_data END
           WHERE affected_table = m[1] AND (before_data ? m[2] OR after_data ? m[2]);
       END LOOP;
   END;
   $$;
   ```

9. **Recria as views** com os mesmos nomes de coluna de saída (`batch_id`, `product_id`, `chain_id`, `calculated_at`). O SQL completo está no [plan da 006](../006-views-procedures/plan.md#2-views):

   ```sql
   CAST(ce.created_at AT TIME ZONE 'America/Sao_Paulo' AS DATE) AS calculated_at
   ```

   Com isso, quem lê as views não muda: `CarbonEmissionJpaRepository` (detalhe do relatório), recomendação, gráfico e a futura procedure da 006. O mês da `vw_monthly_emission` sai em São Paulo sem alterar o texto dela.

MySQL (nota, RN-09): `TIMESTAMPTZ` → `TIMESTAMP` em UTC, `AT TIME ZONE` → `CONVERT_TZ`, `GIN` por expressão → `FULLTEXT` sobre coluna gerada, `IS DISTINCT FROM` → `NOT (NEW <=> OLD)` coluna a coluna.

## 2. Código

### 2.1 Fuso

`SupplyChainVerdeApplication.main`: `TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))` antes do `SpringApplication.run`. Essa linha cobre `LocalDateTime.now()` nos casos de uso, a conversão `TIMESTAMPTZ ↔ LocalDateTime` do Hibernate e o Jackson, sem configurar cada um.

### 2.2 Entidades JPA

Os nomes dos campos Java **não mudam**. Muda só o `@Column`. Assim, JPQL, derived queries (`findBySupplierSupplierId`), `ReportRepositoryImpl` e `AuditLogRepositoryImpl` continuam iguais.

| Entidade | Mudança |
|---|---|
| As 11 `*JpaEntity` | `@Column(name = "<t>_id")` → `@Column(name = "id")` no `@Id` |
| `SupplierJpaEntity.registeredAt` | `created_at`, tipo `LocalDateTime`, `@CreationTimestamp`, `updatable = false` |
| `CarbonEmissionJpaEntity.calculatedAt` | idem |
| `ReportJpaEntity.generatedAt` | `created_at` |
| `AuditLogJpaEntity.performedAt` | `created_at` |
| `UserJpaEntity.createdAt` | sem mudança de nome (o tipo `LocalDateTime` lê `TIMESTAMPTZ` no fuso da JVM) |

`updated_at` não é mapeado: nenhuma rota expõe essa coluna, e o banco a mantém sozinho.

`@CreationTimestamp` faz o valor voltar preenchido no `save`. Antes, `SupplierJpaEntity.registeredAt` era `insertable = false` e voltava nulo até a próxima leitura.

### 2.3 Mappers

Os domínios `Supplier.registeredAt` e `CarbonEmission.calculatedAt` continuam `LocalDate`. Em `SupplierJpaMapper` e `CarbonEmissionJpaMapper`, no mesmo estilo do `map(EmissionFactor)` que já existe:

```java
default LocalDate map(LocalDateTime value) {
    return value == null ? null : value.toLocalDate();
}
```

No `toJpaEntity`, entra `@Mapping(target = "registeredAt" / "calculatedAt", ignore = true)`, porque quem preenche é o `@CreationTimestamp`. O `.calculatedAt(LocalDate.now())` de `CalculateCarbonEmissionUseCase` passa a ser ignorado e pode sair.

### 2.4 Queries

| Arquivo | Mudança |
|---|---|
| `SupplierJpaRepository` (3 queries) | `s.supplier_id` → `s.id`. `s.name_search` e `s.cnpj_digits` → as expressões da `V34`. |
| `ProductJpaRepository` (2 queries) | `p.product_id` → `p.id`. `p.search_vector` → a expressão da `V34`. |
| `BatchJpaRepository` (2 queries) | `b.batch_id`, `p.product_id`, `s.supplier_id` e `c.chain_id` → `id`. O alias de saída `"batchId"` fica. |
| `CarbonEmissionJpaRepository.sumCo2KgByCalculatedAtBetween` | Vira query nativa `select sum(co2_kg) from vw_stage_emission where calculated_at between :startsAt and :endsAt`. A assinatura (`LocalDate`) não muda. Toda emissão tem etapa, lote e produto (FKs `NOT NULL`), então o resultado é o mesmo. |
| `CarbonEmissionJpaRepository` (3 consultas do relatório) | Sem mudança: leem as colunas de saída da view. |

## 3. Testes

| Arquivo | Mudança |
|---|---|
| `src/test/resources/seed/report-seed.sql`, `ReportDetailIT`, `DashboardEmissionsIT`, `SupplierRecommendationIT`, `SupplierUserLinkIT`, `SupplierOwnProfileIT`, `AuditLogTriggerIT` | SQL de preparação com os nomes novos (`id`, `created_at`). As asserções de resposta não mudam (RNF-02). |
| `AuditLogTriggerIT` | Novos casos: `UPDATE` real muda `updated_at`. `UPDATE` sem mudança não muda `updated_at` e não gera linha de auditoria. O snapshot de inserção usa a chave `id`. |
| Um IT de migration | Um registro de auditoria com `supplier_id` gravado antes da `V35` sai com `id` depois dela (`Flyway.configure().target("34")`, insere, migra para `35`). |

Verificação de contrato (RNF-01): salvar o `/v3/api-docs` na `main` e na branch e comparar com `diff`. O resultado tem que ser vazio.
