package br.com.anhembi.supplychainverde.application.port;

import br.com.anhembi.supplychainverde.domain.enums.StageType;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DashboardSummaryReader {
    long countActiveBatches();

    long countCertificationsExpiringBetween(LocalDate startsAt, LocalDate endsAt);

    long countSuppliers();

    BigDecimal sumEmissionsCalculatedBetween(LocalDate startsAt, LocalDate endsAt);

    List<RecentBatch> findRecentBatches(int limit);

    record RecentBatch(
            Long batchId,
            String productName,
            String supplierName,
            BigDecimal quantity,
            ProductUnit unit,
            StageType status
    ) {}
}
