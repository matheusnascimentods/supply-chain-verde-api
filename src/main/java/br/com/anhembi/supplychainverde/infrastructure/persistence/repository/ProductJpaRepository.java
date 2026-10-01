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
            SELECT product_id, name, category, unit, description
            FROM product
            ORDER BY product_id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ProductJpaEntity> findPage(@Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
            SELECT p.product_id, p.name, p.category, p.unit, p.description
            FROM product p
            WHERE p.search_vector @@ websearch_to_tsquery('portuguese', :search)
               OR (:categoryCode <> '' AND lower(p.category::text) = lower(:categoryCode))
            ORDER BY p.product_id ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ProductJpaEntity> findBySearch(
            @Param("search") String search,
            @Param("categoryCode") String categoryCode,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM product p
            WHERE p.search_vector @@ websearch_to_tsquery('portuguese', :search)
               OR (:categoryCode <> '' AND lower(p.category::text) = lower(:categoryCode))
            """, nativeQuery = true)
    long countBySearch(
            @Param("search") String search,
            @Param("categoryCode") String categoryCode
    );
}
