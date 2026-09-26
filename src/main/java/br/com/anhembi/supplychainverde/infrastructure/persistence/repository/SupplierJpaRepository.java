package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.SupplierJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

    boolean existsByCnpj(String cnpj);
}
