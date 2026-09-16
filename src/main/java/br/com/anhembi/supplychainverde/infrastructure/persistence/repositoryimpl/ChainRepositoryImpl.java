package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.ChainJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.ChainJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChainRepositoryImpl implements ChainRepository {
    private final ChainJpaRepository jpaRepository;
    private final ChainJpaMapper mapper;

    @Override
    public Chain save(Chain chain) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(chain)));
    }

    @Override
    public Optional<Chain> findById(Long chainId) {
        return jpaRepository.findById(chainId).map(mapper::toDomain);
    }

    @Override
    public List<Chain> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Chain> findByBatchId(Long batchId) {
        return jpaRepository.findByBatchBatchId(batchId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long chainId) {
        jpaRepository.deleteById(chainId);
    }
}
