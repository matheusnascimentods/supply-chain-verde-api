package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ChainJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChainJpaRepository extends JpaRepository<ChainJpaEntity, Long> {
    List<ChainJpaEntity> findByBatchBatchId(Long batchId);
}
