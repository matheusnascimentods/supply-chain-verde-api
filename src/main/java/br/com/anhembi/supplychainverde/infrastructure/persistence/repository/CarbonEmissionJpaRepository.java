package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CarbonEmissionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface CarbonEmissionJpaRepository extends JpaRepository<CarbonEmissionJpaEntity, Long> {
    @Query("select sum(emission.co2Kg) from CarbonEmissionJpaEntity emission where emission.calculatedAt between :startsAt and :endsAt")
    BigDecimal sumCo2KgByCalculatedAtBetween(@Param("startsAt") LocalDate startsAt, @Param("endsAt") LocalDate endsAt);

    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    Optional<CarbonEmissionJpaEntity> findByChainChainId(Long chainId);

    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product", "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    List<CarbonEmissionJpaEntity> findByChainChainIdIn(Collection<Long> chainIds);

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
