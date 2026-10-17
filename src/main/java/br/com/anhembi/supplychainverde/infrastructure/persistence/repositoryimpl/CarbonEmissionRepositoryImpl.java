package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.CarbonEmissionJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.CarbonEmissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

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
    public List<CarbonEmission> findByChainIds(Collection<Long> chainIds) {
        return jpaRepository.findByChainChainIdIn(chainIds).stream().map(mapper::toDomain).toList();
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
    public void deleteById(Long emissionId) {
        jpaRepository.deleteById(emissionId);
    }

    @Override
    public BigDecimal sumEmissionsCalculatedBetween(LocalDate startsAt, LocalDate endsAt) {
        return Optional.ofNullable(jpaRepository.sumCo2KgByCalculatedAtBetween(startsAt, endsAt)).orElse(BigDecimal.ZERO);
    }

    @Override
    public List<MonthlyEmission> sumMonthly(YearMonth from, YearMonth to, Long supplierId) {
        return jpaRepository.sumMonthly(from.atDay(1), to.atDay(1), supplierId)
                .stream()
                .map(row -> new MonthlyEmission(YearMonth.parse(row.getMonth()), row.getCo2Kg()))
                .toList();
    }

    @Override
    public List<StageEmission> sumByStage(Long supplierId, LocalDate start, LocalDate end) {
        return jpaRepository.sumByStage(supplierId, start, end).stream()
                .map(row -> new StageEmission(StageType.valueOf(row.getStageType()), row.getCo2Kg()))
                .toList();
    }

    @Override
    public List<BatchEmission> sumByBatch(Long supplierId, LocalDate start, LocalDate end) {
        return jpaRepository.sumByBatch(supplierId, start, end).stream()
                .map(row -> new BatchEmission(row.getBatchId(), row.getProductName(), row.getQuantity(),
                        ProductUnit.valueOf(row.getUnit()), row.getCo2Kg()))
                .toList();
    }

    @Override
    public List<MethodEmission> sumByMethod(Long supplierId, LocalDate start, LocalDate end) {
        return jpaRepository.sumByMethod(supplierId, start, end).stream()
                .map(row -> new MethodEmission(CalculationMethod.valueOf(row.getCalculationMethod()),
                        row.getEmissionFactor(), row.getCo2Kg()))
                .toList();
    }
}
