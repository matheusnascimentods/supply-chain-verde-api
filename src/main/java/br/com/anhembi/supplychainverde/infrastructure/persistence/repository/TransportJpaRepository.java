package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.TransportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface TransportJpaRepository extends JpaRepository<TransportJpaEntity, Long> {
    Optional<TransportJpaEntity> findByChainChainId(Long chainId);

    @EntityGraph(attributePaths = {"chain", "chain.batch", "chain.batch.product", "chain.batch.supplier",
            "chain.batch.supplier.address", "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"})
    List<TransportJpaEntity> findByChainChainIdIn(Collection<Long> chainIds);
}
