package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.report.ReportDetailDTO;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetReportUseCase {
    private final ReportRepository reportRepository;
    private final BatchRepository batchRepository;

    @Autowired
    public GetReportUseCase(ReportRepository reportRepository, BatchRepository batchRepository) {
        this.reportRepository = reportRepository;
        this.batchRepository = batchRepository;
    }

    /** Retained for source compatibility with existing direct use-case consumers. */
    public GetReportUseCase(ReportRepository reportRepository) {
        this(reportRepository, null);
    }

    public ReportDetailDTO execute(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relatório não encontrado: " + reportId));
        Supplier supplier = report.getSupplier();
        Long supplierId = supplier == null ? null : supplier.getSupplierId();
        long batchCount = supplierId == null || batchRepository == null
                ? 0
                : batchRepository.countBySupplierIdAndProducedAtBetween(
                        supplierId, report.getPeriodStartAt(), report.getPeriodEndAt());
        return new ReportDetailDTO(
                report.getReportId(),
                supplierId,
                supplier == null || supplier.getCnpj() == null ? null : supplier.getCnpj().formatted(),
                supplier == null ? null : supplier.getName(),
                report.getPeriodStartAt(),
                report.getPeriodEndAt(),
                report.getTotalCo2Kg(),
                batchCount,
                report.getTrackedProductCount(),
                report.getGeneratedAt()
        );
    }
}
