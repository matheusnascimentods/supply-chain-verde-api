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
            SELECT s.*, (SELECT MIN(u.id) FROM users u WHERE u.company_id = s.id) AS user_id
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
                  AND (v.product_id = :productId
                       OR (:productId IS NULL AND v.category = :category AND v.unit = :unit))
            WHERE s.type = 'SUPPLIER'
              AND (:search = ''
               OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:cnpjSearch <> '' AND s.cnpj LIKE CONCAT('%', :cnpjSearch, '%')))
            GROUP BY s.id
            ORDER BY co2KgPerUnit IS NULL, co2KgPerUnit, s.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<SupplierEmissionRank> findRankedByEmission(
            @Param("productId") Long productId,
            @Param("category") String category,
            @Param("unit") String unit,
            @Param("search") String search,
            @Param("booleanSearch") String booleanSearch,
            @Param("cnpjSearch") String cnpjSearch,
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
              AND (:withoutUser = false OR NOT EXISTS (SELECT 1 FROM users u WHERE u.company_id = s.id))
            """, nativeQuery = true)
    long countBySearch(@Param("search") String search, @Param("booleanSearch") String booleanSearch,
            @Param("cnpjSearch") String cnpjSearch,
            @Param("withoutUser") boolean withoutUser);

    @Query(value = """
            SELECT s.id
            FROM company s
            JOIN vw_supplier_sustainability v ON v.supplier_id = s.id
            WHERE s.type = 'SUPPLIER'
              AND (:search = ''
               OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:cnpjSearch <> '' AND s.cnpj LIKE CONCAT('%', :cnpjSearch, '%')))
              AND (:withoutUser = false OR NOT EXISTS (SELECT 1 FROM users u WHERE u.company_id = s.id))
            ORDER BY v.sustainability_score DESC, s.id
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Long> findIdsRankedByScore(
            @Param("search") String search,
            @Param("booleanSearch") String booleanSearch,
            @Param("cnpjSearch") String cnpjSearch,
            @Param("withoutUser") boolean withoutUser,
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

    // ponytail: mercado e fornecedor padrão das escritas que ainda não sabem a companhia; saem quando as
    // Tasks 3 e 4 tirarem a companhia do principal.
    @Query(value = "SELECT MIN(id) FROM company WHERE type = 'MARKET'", nativeQuery = true)
    Long findDefaultMarketId();

    @Query(value = "SELECT MIN(id) FROM company WHERE type = 'SUPPLIER'", nativeQuery = true)
    Long findDefaultSupplierId();

    @EntityGraph(attributePaths = "address")
    Optional<CompanyJpaEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @EntityGraph(attributePaths = "address")
    List<CompanyJpaEntity> findByUserIdIn(Collection<Long> userIds);
}
