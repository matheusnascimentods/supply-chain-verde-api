package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.report.ReportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.report.ReportResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GenerateSustainabilityReportUseCase {
    private final ReportRepository reportRepository;
    private final SupplierRepository supplierRepository;

    public ReportResponseDTO execute(Long supplierId, ReportRequestDTO request) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + supplierId));

        Report report = Report.builder()
                .supplier(supplier)
                .periodStartAt(request.periodStartAt())
                .periodEndAt(request.periodEndAt())
                .totalCo2Kg(BigDecimal.ZERO)
                .trackedProductCount(0)
                .generatedAt(LocalDateTime.now())
                .build();

        Report saved = reportRepository.save(report);
        return new ReportResponseDTO(saved.getReportId(), supplierId, saved.getPeriodStartAt(), saved.getPeriodEndAt(), saved.getTotalCo2Kg(), saved.getTrackedProductCount(), saved.getGeneratedAt());
    }
}
