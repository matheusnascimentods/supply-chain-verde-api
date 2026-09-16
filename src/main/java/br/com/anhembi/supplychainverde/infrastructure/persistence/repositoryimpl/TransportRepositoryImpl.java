package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Transport;
import br.com.anhembi.supplychainverde.domain.repository.TransportRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.TransportJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.TransportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransportRepositoryImpl implements TransportRepository {
    private final TransportJpaRepository jpaRepository;
    private final TransportJpaMapper mapper;

    @Override
    public Transport save(Transport transport) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(transport)));
    }

    @Override
    public Optional<Transport> findById(Long transportId) {
        return jpaRepository.findById(transportId).map(mapper::toDomain);
    }

    @Override
    public Optional<Transport> findByChainId(Long chainId) {
        return jpaRepository.findByChainChainId(chainId).map(mapper::toDomain);
    }

    @Override
    public List<Transport> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long transportId) {
        jpaRepository.deleteById(transportId);
    }
}
