package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.enums.StageType;

import java.math.BigDecimal;
import java.time.LocalDate;
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


    void deleteById(Long emissionId);

    BigDecimal sumEmissionsCalculatedBetween(LocalDate startsAt, LocalDate endsAt);

    List<MonthlyEmission> sumMonthly(YearMonth from, YearMonth to, Long supplierId);

    record MonthlyEmission(YearMonth month, BigDecimal co2Kg) {}

    List<StageEmission> sumByStage(Long supplierId, LocalDate start, LocalDate end);

    List<BatchEmission> sumByBatch(Long supplierId, LocalDate start, LocalDate end);

    List<MethodEmission> sumByMethod(Long supplierId, LocalDate start, LocalDate end);

    record StageEmission(StageType stageType, BigDecimal co2Kg) {}

    record BatchEmission(Long batchId, String productName, BigDecimal quantity, ProductUnit unit, BigDecimal co2Kg) {}

    record MethodEmission(CalculationMethod calculationMethod, BigDecimal emissionFactor, BigDecimal co2Kg) {}
}
