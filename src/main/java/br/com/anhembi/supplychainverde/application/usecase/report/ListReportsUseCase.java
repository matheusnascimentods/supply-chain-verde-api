package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.report.ReportListItemDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListReportsUseCase {
    private final ReportRepository reportRepository;
    private final BatchRepository batchRepository;

    public OffsetPageResponseDTO<ReportListItemDTO> execute(Long supplierId, int limit, int offset) {
        List<Report> reports = reportRepository.findPage(supplierId, limit, offset);
        long totalElements = reportRepository.countPage(supplierId);
        List<ReportListItemDTO> items = reports.stream()
                .map(this::toResponse)
                .toList();
        return OffsetPageResponseDTO.of(items, limit, offset, totalElements);
    }

    private ReportListItemDTO toResponse(Report report) {
        Supplier supplier = report.getSupplier();
        Long supplierId = supplier == null ? null : supplier.getSupplierId();
        long batchCount = supplierId == null ? 0 : batchRepository.countBySupplierIdAndProducedAtBetween(
                supplierId, report.getPeriodStartAt(), report.getPeriodEndAt());
        return new ReportListItemDTO(
                report.getReportId(),
                supplierId,
                supplier == null || supplier.getCnpj() == null ? null : supplier.getCnpj().formatted(),
                supplier == null ? null : supplier.getName(),
                report.getPeriodStartAt(),
                report.getPeriodEndAt(),
                report.getTotalCo2Kg(),
                batchCount,
                report.getGeneratedAt()
        );
    }
}
