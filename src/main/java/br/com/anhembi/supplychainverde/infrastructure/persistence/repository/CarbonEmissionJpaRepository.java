package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CarbonEmissionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CarbonEmissionJpaRepository extends JpaRepository<CarbonEmissionJpaEntity, Long> {
    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    Optional<CarbonEmissionJpaEntity> findByChainChainId(Long chainId);

    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    List<CarbonEmissionJpaEntity> findByChainBatchBatchId(Long batchId);

    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    List<CarbonEmissionJpaEntity> findByChainBatchSupplierSupplierId(Long supplierId);

    @Override
    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    List<CarbonEmissionJpaEntity> findAll();
}
