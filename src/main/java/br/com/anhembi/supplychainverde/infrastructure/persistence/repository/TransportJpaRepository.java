package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.TransportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransportJpaRepository extends JpaRepository<TransportJpaEntity, Long> {
    Optional<TransportJpaEntity> findByChainChainId(Long chainId);
}
