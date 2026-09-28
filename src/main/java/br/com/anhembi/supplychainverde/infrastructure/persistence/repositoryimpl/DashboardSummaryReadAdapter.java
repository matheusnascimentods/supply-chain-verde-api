package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.application.port.DashboardSummaryReader;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.BatchJpaRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.CarbonEmissionJpaRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.CertificationJpaRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.SupplierJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DashboardSummaryReadAdapter implements DashboardSummaryReader {
    private final BatchJpaRepository batchRepository;
    private final CertificationJpaRepository certificationRepository;
    private final SupplierJpaRepository supplierRepository;
    private final CarbonEmissionJpaRepository carbonEmissionRepository;

    @Override
    public long countActiveBatches() {
        return batchRepository.countActiveBatches();
    }

    @Override
    public long countCertificationsExpiringBetween(LocalDate startsAt, LocalDate endsAt) {
        return certificationRepository.countByExpiresAtBetween(startsAt, endsAt);
    }

    @Override
    public long countSuppliers() {
        return supplierRepository.count();
    }

    @Override
    public BigDecimal sumEmissionsCalculatedBetween(LocalDate startsAt, LocalDate endsAt) {
        return Optional.ofNullable(carbonEmissionRepository.sumCo2KgByCalculatedAtBetween(startsAt, endsAt))
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public List<RecentBatch> findRecentBatches(int limit) {
        return batchRepository.findRecentWithLatestStage(PageRequest.of(0, limit)).stream()
                .map(batch -> new RecentBatch(
                        batch.getBatchId(),
                        batch.getProductName(),
                        batch.getSupplierName(),
                        batch.getQuantity(),
                        ProductUnit.valueOf(batch.getUnit()),
                        StageType.valueOf(batch.getStatus())
                ))
                .toList();
    }
}
