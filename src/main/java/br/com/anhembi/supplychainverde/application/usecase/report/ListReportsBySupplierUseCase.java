package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.report.ReportResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListReportsBySupplierUseCase {
    private final ReportRepository reportRepository;

    public List<ReportResponseDTO> execute(Long supplierId) {
        return reportRepository.findBySupplierId(supplierId).stream().map(this::toResponse).toList();
    }

    private ReportResponseDTO toResponse(Report report) {
        return new ReportResponseDTO(report.getReportId(), report.getSupplier() != null ? report.getSupplier().getSupplierId() : null, report.getPeriodStartAt(), report.getPeriodEndAt(), report.getTotalCo2Kg(), report.getTrackedProductCount(), report.getGeneratedAt());
    }
}
