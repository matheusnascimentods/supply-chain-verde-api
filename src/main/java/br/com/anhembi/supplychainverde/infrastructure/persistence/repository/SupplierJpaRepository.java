package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.SupplierJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SupplierJpaRepository extends JpaRepository<SupplierJpaEntity, Long> {
    Optional<SupplierJpaEntity> findByCnpj(String cnpj);
    boolean existsByCnpj(String cnpj);
}
