package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

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
}
