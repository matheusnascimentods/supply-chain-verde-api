package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;

import java.util.List;
import java.util.Optional;

public interface CarbonEmissionRepository {
    CarbonEmission save(CarbonEmission carbonEmission);

    Optional<CarbonEmission> findById(Long emissionId);

    Optional<CarbonEmission> findByChainId(Long chainId);

    List<CarbonEmission> findAll();

    List<CarbonEmission> findByBatchId(Long batchId);

    List<CarbonEmission> findBySupplierId(Long supplierId);

    void deleteById(Long emissionId);
}
