package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ChainJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Collection;

public interface ChainJpaRepository extends JpaRepository<ChainJpaEntity, Long> {
    List<ChainJpaEntity> findByBatchBatchId(Long batchId);

    @EntityGraph(attributePaths = {"batch", "batch.product", "batch.supplier", "batch.supplier.address",
            "originAddress", "destinationAddress", "responsibleUser"})
    List<ChainJpaEntity> findByBatchBatchIdInOrderByStartedAtAscChainIdAsc(Collection<Long> batchIds);
}
