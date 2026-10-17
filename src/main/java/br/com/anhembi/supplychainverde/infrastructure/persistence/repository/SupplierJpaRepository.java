package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository.SupplierEmissionRank;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository.SupplierScore;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.SupplierJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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
            WHERE to_tsvector('portuguese'::regconfig, coalesce(s.name, '')) @@ websearch_to_tsquery('portuguese', :search)
               OR (:cnpjSearch <> '' AND regexp_replace(s.cnpj::text, '[^0-9]', '', 'g') LIKE '%' || :cnpjSearch || '%')
            """, nativeQuery = true)
    List<SupplierJpaEntity> findBySearch(
            @Param("search") String search,
            @Param("cnpjSearch") String cnpjSearch
    );

    boolean existsByCnpj(String cnpj);

    @Query(value = """
            SELECT s.id AS "supplierId",
                   ROUND(SUM(v.total_co2_kg) / NULLIF(SUM(v.total_quantity), 0), 4) AS "co2KgPerUnit"
            FROM supplier s
            LEFT JOIN vw_supplier_product_emission v
                   ON v.supplier_id = s.id
                  AND (v.product_id = CAST(:productId AS BIGINT)
                       OR (CAST(:productId AS BIGINT) IS NULL
                           AND v.category = CAST(:category AS product_category)
                           AND v.unit = CAST(:unit AS product_unit)))
            WHERE :search = ''
               OR to_tsvector('portuguese'::regconfig, coalesce(s.name, '')) @@ websearch_to_tsquery('portuguese', :search)
               OR (:cnpjSearch <> '' AND regexp_replace(s.cnpj::text, '[^0-9]', '', 'g') LIKE '%' || :cnpjSearch || '%')
            GROUP BY s.id
            ORDER BY "co2KgPerUnit" ASC NULLS LAST, s.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<SupplierEmissionRank> findRankedByEmission(
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
            WHERE (:search = ''
               OR to_tsvector('portuguese'::regconfig, coalesce(s.name, '')) @@ websearch_to_tsquery('portuguese', :search)
               OR (:cnpjSearch <> '' AND regexp_replace(s.cnpj::text, '[^0-9]', '', 'g') LIKE '%' || :cnpjSearch || '%'))
              AND (:withoutUser = false OR s.user_id IS NULL)
            """, nativeQuery = true)
    long countBySearch(@Param("search") String search, @Param("cnpjSearch") String cnpjSearch,
            @Param("withoutUser") boolean withoutUser);

    @Query(value = """
            SELECT s.id
            FROM supplier s
            JOIN vw_supplier_sustainability v ON v.supplier_id = s.id
            WHERE (:search = ''
               OR to_tsvector('portuguese'::regconfig, coalesce(s.name, '')) @@ websearch_to_tsquery('portuguese', :search)
               OR (:cnpjSearch <> '' AND regexp_replace(s.cnpj::text, '[^0-9]', '', 'g') LIKE '%' || :cnpjSearch || '%'))
              AND (:withoutUser = false OR s.user_id IS NULL)
            ORDER BY v.sustainability_score DESC, s.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Long> findIdsRankedByScore(
            @Param("search") String search,
            @Param("cnpjSearch") String cnpjSearch,
            @Param("withoutUser") boolean withoutUser,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT supplier_id AS "supplierId",
                   sustainability_score AS "sustainabilityScore",
                   CAST(active_certifications AS INTEGER) AS "activeCertificationCount",
                   total_co2_kg AS "totalCo2Kg"
            FROM vw_supplier_sustainability
            WHERE supplier_id IN (:supplierIds)
            """, nativeQuery = true)
    List<SupplierScore> findScoresByIds(@Param("supplierIds") Collection<Long> supplierIds);

    @EntityGraph(attributePaths = "address")
    Optional<SupplierJpaEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @EntityGraph(attributePaths = "address")
    List<SupplierJpaEntity> findByUserIdIn(Collection<Long> userIds);
}
