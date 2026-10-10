package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.SupplierJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, Long> {
    @Override
    @EntityGraph(attributePaths = "address")
    Optional<SupplierJpaEntity> findById(Long supplierId);

    @EntityGraph(attributePaths = "address")
    Optional<SupplierJpaEntity> findByCnpj(String cnpj);

    @Override
    @EntityGraph(attributePaths = "address")
    List<SupplierJpaEntity> findAll();

    @Query(value = """
            SELECT s.*
            FROM supplier s
            WHERE s.name_search @@ websearch_to_tsquery('portuguese', :search)
               OR (:cnpjSearch <> '' AND s.cnpj_digits LIKE '%' || :cnpjSearch || '%')
            """, nativeQuery = true)
    List<SupplierJpaEntity> findBySearch(
            @Param("search") String search,
            @Param("cnpjSearch") String cnpjSearch
    );

    boolean existsByCnpj(String cnpj);

    @Query(value = """
            SELECT s.supplier_id AS "supplierId",
                   ROUND(SUM(v.total_co2_kg) / NULLIF(SUM(v.total_quantity), 0), 4) AS "co2KgPerUnit"
            FROM supplier s
            LEFT JOIN vw_supplier_product_emission v
                   ON v.supplier_id = s.supplier_id
                  AND (v.product_id = CAST(:productId AS BIGINT)
                       OR (CAST(:productId AS BIGINT) IS NULL
                           AND v.category = CAST(:category AS product_category)
                           AND v.unit = CAST(:unit AS product_unit)))
            WHERE :search = ''
               OR s.name_search @@ websearch_to_tsquery('portuguese', :search)
               OR (:cnpjSearch <> '' AND s.cnpj_digits LIKE '%' || :cnpjSearch || '%')
            GROUP BY s.supplier_id
            ORDER BY "co2KgPerUnit" ASC NULLS LAST, s.supplier_id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<SupplierEmissionRankProjection> findRankedByEmission(
            @Param("productId") Long productId,
            @Param("category") String category,
            @Param("unit") String unit,
            @Param("search") String search,
            @Param("cnpjSearch") String cnpjSearch  ,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM supplier s
            WHERE :search = ''
               OR s.name_search @@ websearch_to_tsquery('portuguese', :search)
               OR (:cnpjSearch <> '' AND s.cnpj_digits LIKE '%' || :cnpjSearch || '%')
            """, nativeQuery = true)
    long countBySearch(@Param("search") String search, @Param("cnpjSearch") String cnpjSearch);
}
