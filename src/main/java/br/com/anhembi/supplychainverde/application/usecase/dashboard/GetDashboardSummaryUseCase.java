package br.com.anhembi.supplychainverde.application.usecase.dashboard;

import br.com.anhembi.supplychainverde.application.dto.dashboard.DashboardSummaryResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.dashboard.RecentBatchSummaryDTO;
import br.com.anhembi.supplychainverde.application.port.DashboardSummaryReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetDashboardSummaryUseCase {
    private final DashboardSummaryReader dashboardSummaryReader;

    public DashboardSummaryResponseDTO execute(Integer requestedLimit) {
        int limit = requestedLimit == null ? 10 : requestedLimit;
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        return new DashboardSummaryResponseDTO(
                dashboardSummaryReader.countActiveBatches(),
                dashboardSummaryReader.countCertificationsExpiringBetween(today, today.plusDays(30)),
                dashboardSummaryReader.countSuppliers(),
                dashboardSummaryReader.sumEmissionsCalculatedBetween(monthStart, monthEnd),
                dashboardSummaryReader.findRecentBatches(limit).stream()
                        .map(batch -> new RecentBatchSummaryDTO(
                                batch.batchId(),
                                batch.productName(),
                                batch.supplierName(),
                                batch.quantity(),
                                batch.unit(),
                                batch.status()
                        ))
                        .toList()
        );
    }
}
