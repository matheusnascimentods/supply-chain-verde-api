package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {
    @Query(value = "SELECT COUNT(*) FROM product", nativeQuery = true)
    long countAllProducts();

    @Query(value = """
            SELECT id, name, category, unit, description
            FROM product
            ORDER BY id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ProductJpaEntity> findPage(@Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
            SELECT p.id, p.name, p.category, p.unit, p.description
            FROM product p
            WHERE MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:categoryCode <> '' AND p.category = :categoryCode)
            ORDER BY p.id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ProductJpaEntity> findBySearch(
            @Param("booleanSearch") String booleanSearch,
            @Param("categoryCode") String categoryCode,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM product p
            WHERE MATCH(p.name, p.description) AGAINST (:booleanSearch IN BOOLEAN MODE)
               OR (:categoryCode <> '' AND p.category = :categoryCode)
            """, nativeQuery = true)
    long countBySearch(
            @Param("booleanSearch") String booleanSearch,
            @Param("categoryCode") String categoryCode
    );
}
