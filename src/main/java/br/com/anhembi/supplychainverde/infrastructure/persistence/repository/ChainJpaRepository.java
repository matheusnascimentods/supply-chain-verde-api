package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ChainJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

public interface ChainJpaRepository extends JpaRepository<ChainJpaEntity, Long> {
    List<ChainJpaEntity> findByBatchBatchId(Long batchId);

    @EntityGraph(attributePaths = {"batch", "batch.product", "batch.supplier", "batch.supplier.address",
            "destinationAddress", "responsibleUser"})
    List<ChainJpaEntity> findByBatchBatchIdInOrderByStartedAtAscChainIdAsc(Collection<Long> batchIds);

    @Modifying
    @Query("update ChainJpaEntity c set c.endedAt = :endedAt where c.batch.batchId = :batchId and c.endedAt is null")
    void closeOpenStages(Long batchId, LocalDateTime endedAt);
}
