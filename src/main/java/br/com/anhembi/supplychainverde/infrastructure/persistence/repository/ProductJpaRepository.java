package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.domain.repository.ProductRepository.CatalogItem;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {
    @Query(value = "SELECT COUNT(*) FROM product WHERE (:supplierId IS NULL OR supplier_id = :supplierId)", nativeQuery = true)
    long countAllProducts(@Param("supplierId") Long supplierId);

    @Query(value = """
            SELECT id, name, category, unit, description, supplier_id
            FROM product
            WHERE (:supplierId IS NULL OR supplier_id = :supplierId)
            ORDER BY id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ProductJpaEntity> findPage(@Param("supplierId") Long supplierId, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
            SELECT p.id, p.name, p.category, p.unit, p.description, p.supplier_id
            FROM product p
            WHERE (:supplierId IS NULL OR p.supplier_id = :supplierId)
              AND (MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:categoryCode <> '' AND p.category = :categoryCode))
            ORDER BY p.id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ProductJpaEntity> findBySearch(
            @Param("supplierId") Long supplierId,
            @Param("booleanSearch") String booleanSearch,
            @Param("categoryCode") String categoryCode,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM product p
            WHERE (:supplierId IS NULL OR p.supplier_id = :supplierId)
              AND (MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:categoryCode <> '' AND p.category = :categoryCode))
            """, nativeQuery = true)
    long countBySearch(
            @Param("supplierId") Long supplierId,
            @Param("booleanSearch") String booleanSearch,
            @Param("categoryCode") String categoryCode
    );

    // RN-05: produto e fornecedor são casados cada um inteiro, nunca as palavras espalhadas entre os dois.
    String CATALOG_WHERE = """
            WHERE (:supplierId IS NULL OR p.supplier_id = :supplierId)
              AND (:category IS NULL OR p.category = :category)
              AND (:unit IS NULL OR p.unit = :unit)
              AND ((:booleanSearch = '' AND :categoryCode = '')
               OR MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR MATCH(c.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:categoryCode <> '' AND p.category = :categoryCode))
            """;

    // RN-03: a data do aceite não é gravada, então vale created_at (fuso do servidor, America/Sao_Paulo).
    // RN-04: mesma conta de findRankedByEmission.
    // ponytail: agrega a view inteira por página; materializar por (supplier, category, unit) se o catálogo passar de alguns milhares de produtos.
    @Query(value = """
            SELECT p.id AS productId, p.supplier_id AS supplierId, p.name AS name, p.category AS category,
                   p.unit AS unit, p.description AS description, c.name AS supplierName,
                   COALESCE(recent.n, 0) AS acceptedBatchesLast30Days,
                   co2.co2_kg_per_unit AS co2KgPerUnit
            FROM product p
            JOIN company c ON c.id = p.supplier_id
            LEFT JOIN (SELECT product_id, COUNT(*) AS n
                       FROM batch
                       WHERE (:marketId IS NULL OR market_id = :marketId)
                         AND status = 'ACTIVE'
                         AND created_at >= NOW() - INTERVAL 30 DAY
                       GROUP BY product_id) recent ON recent.product_id = p.id
            LEFT JOIN (SELECT supplier_id, category, unit,
                              ROUND(SUM(total_co2_kg) / NULLIF(SUM(total_quantity), 0), 4) AS co2_kg_per_unit
                       FROM vw_supplier_product_emission
                       GROUP BY supplier_id, category, unit) co2
                   ON co2.supplier_id = p.supplier_id AND co2.category = p.category AND co2.unit = p.unit
            """ + CATALOG_WHERE + """
            ORDER BY acceptedBatchesLast30Days DESC, co2KgPerUnit IS NULL, co2KgPerUnit, p.name, p.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<CatalogItem> findCatalog(
            @Param("supplierId") Long supplierId,
            @Param("marketId") Long marketId,
            @Param("category") String category,
            @Param("unit") String unit,
            @Param("booleanSearch") String booleanSearch,
            @Param("categoryCode") String categoryCode,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = "SELECT COUNT(*) FROM product p JOIN company c ON c.id = p.supplier_id\n" + CATALOG_WHERE, nativeQuery = true)
    long countCatalog(
            @Param("supplierId") Long supplierId,
            @Param("category") String category,
            @Param("unit") String unit,
            @Param("booleanSearch") String booleanSearch,
            @Param("categoryCode") String categoryCode
    );
}
