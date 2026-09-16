package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CarbonEmissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CarbonEmissionJpaRepository extends JpaRepository<CarbonEmissionJpaEntity, Long> {
    Optional<CarbonEmissionJpaEntity> findByChainChainId(Long chainId);
    List<CarbonEmissionJpaEntity> findByChainBatchBatchId(Long batchId);
    List<CarbonEmissionJpaEntity> findByChainBatchSupplierSupplierId(Long supplierId);
}
