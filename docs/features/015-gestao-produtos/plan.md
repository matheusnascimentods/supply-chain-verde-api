# Plano técnico — Gestão de produtos no pedido de lote

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Onde fica

O pedido usa o `GET /products` que já existe. Hoje o mercado só usa essa rota no modal de lote ("catálogo... usado no modal de lote", `ProductController`), e não tem tela de produtos. Por isso, mudar a ordem e acrescentar campos **só no ramo do mercado** não quebra nenhuma tela, e não precisa de rota nova.

O ramo da companhia fornecedora (`principal.isSupplierCompany()`) continua com `findPage`/`findBySearch` (RN-07).

## 2. Query do catálogo (`ProductJpaRepository.findCatalog` / `countCatalog`)

```sql
SELECT p.id AS productId, p.supplier_id AS supplierId, p.name AS name, p.category AS category,
       p.unit AS unit, p.description AS description, c.name AS supplierName,
       COALESCE(recent.n, 0) AS acceptedBatchesLast30Days,
       co2.co2_kg_per_unit AS co2KgPerUnit
FROM product p
JOIN company c ON c.id = p.supplier_id
LEFT JOIN (SELECT product_id, COUNT(*) AS n
           FROM batch
           WHERE (:marketId IS NULL OR market_id = :marketId)          -- RN-03
             AND status = 'ACTIVE'
             AND created_at >= NOW() - INTERVAL 30 DAY
           GROUP BY product_id) recent ON recent.product_id = p.id
LEFT JOIN (SELECT supplier_id, category, unit,                          -- RN-04, a mesma conta de findRankedByEmission
                  ROUND(SUM(total_co2_kg) / NULLIF(SUM(total_quantity), 0), 4) AS co2_kg_per_unit
           FROM vw_supplier_product_emission
           GROUP BY supplier_id, category, unit) co2
       ON co2.supplier_id = p.supplier_id AND co2.category = p.category AND co2.unit = p.unit
WHERE (:category IS NULL OR p.category = :category)                   -- RN-06
  AND (:unit IS NULL OR p.unit = :unit)
  AND ((:booleanSearch = '' AND :categoryCode = '')                   -- RN-05
       OR MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
       OR MATCH(c.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
       OR (:categoryCode <> '' AND p.category = :categoryCode))
ORDER BY acceptedBatchesLast30Days DESC, co2KgPerUnit IS NULL, co2KgPerUnit, p.name, p.id   -- RN-02
LIMIT :limit OFFSET :offset
```

- O `countCatalog` usa o mesmo `FROM`/`WHERE`, sem os dois `LEFT JOIN` e sem o `ORDER BY`.
- `NOW()` e `created_at` estão no fuso do servidor (`America/Sao_Paulo`), então a janela de 30 dias é a do relógio de Brasília.
- Índice para o subselect `recent`, em `V15__idx_batch_market_status_created.sql`:

  ```sql
  CREATE INDEX idx_batch_market_status_created ON batch (market_id, status, created_at);
  ```

  Ele também cobre o filtro `market_id`+`status` da listagem de lotes.
- O agregado de CO₂ lê a view inteira a cada página. Com poucas dezenas de fornecedores isso custa milissegundos. Teto, anotado no código: `// ponytail: agrega a view inteira por página; materializar por (supplier, category, unit) se o catálogo passar de alguns milhares de produtos.`

## 3. Camadas

O padrão é o mesmo do `SupplierEmissionRank` do `CompanyRepository`: uma projeção declarada no repositório de domínio e implementada pelo Spring Data.

```java
// ProductRepository (domínio)
interface CatalogItem {
    Long getProductId(); Long getSupplierId(); String getName(); ProductCategory getCategory();
    ProductUnit getUnit(); String getDescription(); String getSupplierName();
    BigDecimal getCo2KgPerUnit(); Long getAcceptedBatchesLast30Days();
}
List<CatalogItem> findCatalog(Long marketId, ProductCategory category, ProductUnit unit, String search, String categoryCode, int limit, int offset);
long countCatalog(Long marketId, ProductCategory category, ProductUnit unit, String search, String categoryCode);
```

- `ProductRepositoryImpl` converte `search` com `FullTextSearch.booleanQuery` (feature 014), e os enums com `name()`.
- `ProductResponseDTO` ganha `supplierName`, `co2KgPerUnit` e `acceptedBatchesLast30Days`, que vêm `null` no ramo do fornecedor. O `ProductDtoMapper` ganha `toResponse(CatalogItem)`.
- `ListProductsUseCase.execute(limit, offset, search, supplierId, marketId, category, unit, isSupplierCompany)`:
  - fornecedor: o código de hoje;
  - mercado/admin: `findCatalog`/`countCatalog`, com o mesmo `categoryCode(term)`.
  - Se `supplierId` vier no ramo do mercado, ele continua filtrando: entra como `(:supplierId IS NULL OR p.supplier_id = :supplierId)` na query do catálogo, e o contrato não quebra.
- `ProductController.list` recebe `@RequestParam(required = false) ProductCategory category` e `ProductUnit unit`, e passa `principal.marketId()` (como o `BatchController`).

## 4. Remoção da recomendação por categoria/unidade (RN-09)

Só depois que o web parar de chamar a rota (web Tasks 2 e 3):

- `SupplierController`: saem `category`/`unit` e a validação `(category == null) != (unit == null)`;
- `RankSuppliersBySustainabilityUseCase`: sai o ramo da recomendação;
- `CompanyRepository`/`CompanyRepositoryImpl`/`CompanyJpaRepository`: saem `findRankedByEmission` e `SupplierEmissionRank`;
- `SupplierRecommendationIT`: os casos de recomendação viram casos de ordem em `ProductControllerIT`, e o arquivo sai.

Antes de apagar, rodar `grep -rn "findRankedByEmission\|SupplierEmissionRank\|co2KgPerUnit"` para confirmar que nada mais usa esses nomes.

## 5. Testes

`ProductControllerIT` (Testcontainers), com lotes criados no próprio teste:

- ordem por lotes `ACTIVE` do mercado nos 30 dias. `PENDING`, `REJECTED`, lote com mais de 30 dias e lote de outro mercado não contam (RN-02, RN-03); para o lote antigo, `UPDATE batch SET created_at = NOW() - INTERVAL 31 DAY`;
- desempate por `co2KgPerUnit`, com `null` no fim (RN-04);
- `search` por nome do fornecedor, por produto sem acento e pelo rótulo da categoria (RN-05);
- `category` sozinho, `unit` sozinho e os dois (RN-06);
- companhia fornecedora: só os dela, com os campos novos `null` (RN-07).
