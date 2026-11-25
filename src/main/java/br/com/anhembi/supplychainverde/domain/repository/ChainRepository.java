package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Chain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ChainRepository {
    Chain save(Chain chain);

    Optional<Chain> findById(Long chainId);

    List<Chain> findAll();

    List<Chain> findByBatchId(Long batchId);

    List<Chain> findByBatchIds(Collection<Long> batchIds);

    void deleteById(Long chainId);

    /** Encerra as etapas ainda abertas do lote: uma etapa termina quando a próxima começa. */
    void closeOpenStages(Long batchId, LocalDateTime endedAt);
}
