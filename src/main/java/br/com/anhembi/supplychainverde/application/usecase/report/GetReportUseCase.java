package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.report.ReportResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetReportUseCase {
    private final ReportRepository reportRepository;

    public ReportResponseDTO execute(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Relatório não encontrado: " + reportId));
        return new ReportResponseDTO(report.getReportId(), report.getSupplier() != null ? report.getSupplier().getSupplierId() : null, report.getPeriodStartAt(), report.getPeriodEndAt(), report.getTotalCo2Kg(), report.getTrackedProductCount(), report.getGeneratedAt());
    }
}
