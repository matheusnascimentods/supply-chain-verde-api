package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface CarbonEmissionRepository {
    CarbonEmission save(CarbonEmission carbonEmission);

    Optional<CarbonEmission> findById(Long emissionId);

    Optional<CarbonEmission> findByChainId(Long chainId);

    List<CarbonEmission> findByChainIds(Collection<Long> chainIds);

    List<CarbonEmission> findAll();

    List<CarbonEmission> findByBatchId(Long batchId);

    List<CarbonEmission> findBySupplierId(Long supplierId);

    void deleteById(Long emissionId);

    List<MonthlyEmission> sumMonthly(YearMonth from, YearMonth to, Long supplierId);

    record MonthlyEmission(YearMonth month, BigDecimal co2Kg) {}
}
