package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.CarbonEmissionJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.CarbonEmissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CarbonEmissionRepositoryImpl implements CarbonEmissionRepository {
    private final CarbonEmissionJpaRepository jpaRepository;
    private final CarbonEmissionJpaMapper mapper;

    @Override
    public CarbonEmission save(CarbonEmission carbonEmission) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(carbonEmission)));
    }

    @Override
    public Optional<CarbonEmission> findById(Long emissionId) {
        return jpaRepository.findById(emissionId).map(mapper::toDomain);
    }

    @Override
    public Optional<CarbonEmission> findByChainId(Long chainId) {
        return jpaRepository.findByChainChainId(chainId).map(mapper::toDomain);
    }

    @Override
    public List<CarbonEmission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CarbonEmission> findByBatchId(Long batchId) {
        return jpaRepository.findByChainBatchBatchId(batchId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CarbonEmission> findBySupplierId(Long supplierId) {
        return jpaRepository.findByChainBatchSupplierSupplierId(supplierId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long emissionId) {
        jpaRepository.deleteById(emissionId);
    }
}
