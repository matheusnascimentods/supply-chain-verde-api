# Plano técnico — Recomendação de fornecedores sustentáveis

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md). Depende de `vw_supplier_product_emission` ([views-procedures](../views-procedures/plan.md)).

## 1. Consulta

O ranking atual calcula o score em Java (`SustainabilityScoreCalculator`) depois de buscar a página. Para ordenar a lista inteira pelo CO₂ por unidade, a ordenação precisa acontecer na consulta.

```sql
-- productId
SELECT s.supplier_id, v.co2_kg_per_unit
FROM supplier s
LEFT JOIN vw_supplier_product_emission v
       ON v.supplier_id = s.supplier_id AND v.product_id = :productId
WHERE (:search IS NULL OR s.name ILIKE '%' || :search || '%')
ORDER BY v.co2_kg_per_unit ASC NULLS LAST, s.supplier_id
LIMIT :limit OFFSET :offset;

-- category + unit: soma por fornecedor antes de dividir (média ponderada, não média das médias)
SELECT s.supplier_id, SUM(v.total_co2_kg) / SUM(v.total_quantity) AS co2_kg_per_unit
FROM supplier s
LEFT JOIN vw_supplier_product_emission v
       ON v.supplier_id = s.supplier_id AND v.category = :category AND v.unit = :unit
WHERE ...
GROUP BY s.supplier_id
ORDER BY co2_kg_per_unit ASC NULLS LAST, s.supplier_id
LIMIT :limit OFFSET :offset;
```

O desempate por `sustainabilityScore` (RN-04/RN-05) é aplicado em Java na página retornada, porque o score não está no banco. Como empates exatos de CO₂ são raros e os sem histórico vêm agrupados no fim, a diferença de ordem entre páginas é aceitável. Se o score for para uma view no futuro, o desempate desce para o `ORDER BY`.

## 2. Camadas

| Item | Mudança |
|---|---|
| `SupplierController.getSuppliers` | Novos `@RequestParam` opcionais `productId`, `category` (`ProductCategory`), `unit` (`ProductUnit`); validação de combinação (RF-02). |
| `RankSuppliersBySustainabilityUseCase` | Novo caminho `executeRecommended(criteria, limit, offset, search)`: busca a página ordenada, completa os campos atuais (certificações, score, relatórios) para os ids da página e preserva a ordem. |
| `SupplierRepository` | Duas consultas nativas acima + contagem para `totalPages`. |
| `SupplierRankingDTO` | Campo `BigDecimal co2KgPerUnit` (nulo no ranking comum). |
| Swagger | Descrever os parâmetros e a ordem. |

## 3. Testes

- Fornecedor A (0,4 kg/un) antes de B (0,6 kg/un) antes de C (sem lotes).
- Categoria+unidade: produto em `TON` não entra na busca `KG`.
- Média ponderada: A com lote pequeno muito limpo e lote grande sujo é avaliado pelo total.
- 400 para combinações inválidas; ranking sem parâmetros igual ao atual.
