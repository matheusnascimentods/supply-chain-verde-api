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

    @EntityGraph(attributePaths = "address")
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
}
