package br.com.anhembi.supplychainverde.application.usecase.dashboard;

import br.com.anhembi.supplychainverde.application.dto.dashboard.DashboardSummaryResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.dashboard.RecentBatchSummaryDTO;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetDashboardSummaryUseCase {
    private final BatchRepository batchRepository;
    private final CertificationRepository certificationRepository;
    private final SupplierRepository supplierRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;

    public DashboardSummaryResponseDTO execute(Integer requestedLimit) {
        int limit = requestedLimit == null ? 10 : requestedLimit;
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        return new DashboardSummaryResponseDTO(
                batchRepository.countActiveBatches(),
                certificationRepository.countByExpiresAtBetween(today, today.plusDays(30)),
                supplierRepository.countBySearch("", false),
                carbonEmissionRepository.sumEmissionsCalculatedBetween(monthStart, monthEnd),
                batchRepository.findRecentBatches(limit).stream()
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
