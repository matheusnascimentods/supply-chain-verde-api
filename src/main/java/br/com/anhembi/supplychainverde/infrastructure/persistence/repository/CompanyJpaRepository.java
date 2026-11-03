package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository.SupplierEmissionRank;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository.SupplierScore;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CompanyJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, Long> {
    @Override
    @EntityGraph(attributePaths = "address")
    Optional<CompanyJpaEntity> findById(Long companyId);

    @EntityGraph(attributePaths = "address")
    Optional<CompanyJpaEntity> findByCompanyIdAndType(Long companyId, CompanyType type);

    @EntityGraph(attributePaths = "address")
    List<CompanyJpaEntity> findAllByType(CompanyType type);

    @Query(value = """
            SELECT s.*
            FROM company s
            WHERE s.type = 'SUPPLIER'
              AND (MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:cnpjSearch <> '' AND s.cnpj LIKE CONCAT('%', :cnpjSearch, '%')))
            """, nativeQuery = true)
    List<CompanyJpaEntity> findBySearch(
            @Param("booleanSearch") String booleanSearch,
            @Param("cnpjSearch") String cnpjSearch
    );

    boolean existsByCnpj(String cnpj);

    @Query(value = """
            SELECT s.id AS supplierId,
                   ROUND(SUM(v.total_co2_kg) / NULLIF(SUM(v.total_quantity), 0), 4) AS co2KgPerUnit
            FROM company s
            LEFT JOIN vw_supplier_product_emission v
                   ON v.supplier_id = s.id
                  AND v.category = :category AND v.unit = :unit
            WHERE s.type = 'SUPPLIER'
              AND (:search = ''
               OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:cnpjSearch <> '' AND s.cnpj LIKE CONCAT('%', :cnpjSearch, '%')))
              AND (:workedWithOnly = false OR EXISTS (SELECT 1 FROM batch b WHERE b.supplier_id = s.id AND b.market_id = :marketId))
            GROUP BY s.id
            ORDER BY co2KgPerUnit IS NULL, co2KgPerUnit, s.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<SupplierEmissionRank> findRankedByEmission(
            @Param("category") String category,
            @Param("unit") String unit,
            @Param("search") String search,
            @Param("booleanSearch") String booleanSearch,
            @Param("cnpjSearch") String cnpjSearch,
            @Param("workedWithOnly") boolean workedWithOnly,
            @Param("marketId") Long marketId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM company s
            WHERE s.type = 'SUPPLIER'
              AND (:search = ''
               OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:cnpjSearch <> '' AND s.cnpj LIKE CONCAT('%', :cnpjSearch, '%')))
              AND (:workedWithOnly = false OR EXISTS (SELECT 1 FROM batch b WHERE b.supplier_id = s.id AND b.market_id = :marketId))
            """, nativeQuery = true)
    long countBySearch(@Param("search") String search, @Param("booleanSearch") String booleanSearch,
            @Param("cnpjSearch") String cnpjSearch,
            @Param("workedWithOnly") boolean workedWithOnly,
            @Param("marketId") Long marketId);

    @Query(value = """
            SELECT s.id
            FROM company s
            JOIN vw_supplier_sustainability v ON v.supplier_id = s.id
            WHERE s.type = 'SUPPLIER'
              AND (:search = ''
               OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:cnpjSearch <> '' AND s.cnpj LIKE CONCAT('%', :cnpjSearch, '%')))
              AND (:workedWithOnly = false OR EXISTS (SELECT 1 FROM batch b WHERE b.supplier_id = s.id AND b.market_id = :marketId))
            ORDER BY v.sustainability_score DESC, s.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Long> findIdsRankedByScore(
            @Param("search") String search,
            @Param("booleanSearch") String booleanSearch,
            @Param("cnpjSearch") String cnpjSearch,
            @Param("workedWithOnly") boolean workedWithOnly,
            @Param("marketId") Long marketId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT supplier_id AS supplierId,
                   sustainability_score AS sustainabilityScore,
                   active_certifications AS activeCertificationCount,
                   total_co2_kg AS totalCo2Kg
            FROM vw_supplier_sustainability
            WHERE supplier_id IN (:supplierIds)
            """, nativeQuery = true)
    List<SupplierScore> findScoresByIds(@Param("supplierIds") Collection<Long> supplierIds);

    // Glossário: fornecedores do mercado são os que têm ou tiveram lote com ele, em qualquer status.
    @Query(value = "SELECT DISTINCT b.supplier_id FROM batch b WHERE b.market_id = :marketId AND b.supplier_id IN (:supplierIds)",
            nativeQuery = true)
    List<Long> findWorkedWithIds(@Param("marketId") Long marketId, @Param("supplierIds") Collection<Long> supplierIds);

}
