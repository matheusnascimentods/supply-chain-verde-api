package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.report.ReportDetailDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierCertificationDTO;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GetReportUseCase {
    private final ReportRepository reportRepository;
    private final BatchRepository batchRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final CertificationRepository certificationRepository;

    public GetReportUseCase(ReportRepository reportRepository, BatchRepository batchRepository,
                            CarbonEmissionRepository carbonEmissionRepository,
                            CertificationRepository certificationRepository) {
        this.reportRepository = reportRepository;
        this.batchRepository = batchRepository;
        this.carbonEmissionRepository = carbonEmissionRepository;
        this.certificationRepository = certificationRepository;
    }

    public ReportDetailDTO execute(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Relatório não encontrado: " + reportId));
        Supplier supplier = report.getSupplier();
        Long supplierId = supplier == null ? null : supplier.getSupplierId();
        LocalDate start = report.getPeriodStartAt();
        LocalDate end = report.getPeriodEndAt();
        boolean hasSupplier = supplierId != null;
        return new ReportDetailDTO(
                report.getReportId(),
                supplierId,
                supplier == null || supplier.getCnpj() == null ? null : supplier.getCnpj().formatted(),
                supplier == null ? null : supplier.getName(),
                start,
                end,
                report.getTotalCo2Kg(),
                hasSupplier ? batchRepository.countBySupplierIdAndProducedAtBetween(supplierId, start, end) : 0,
                report.getTrackedProductCount(),
                report.getGeneratedAt(),
                supplier == null ? null : new ReportDetailDTO.Supplier(
                        supplier.getPhone(), AddressResponseDTO.from(supplier.getAddress())),
                hasSupplier ? emissions(supplierId, start, end) : emptyEmissions(),
                hasSupplier
                        ? certificationRepository.findBySupplierIdOverlapping(supplierId, start, end).stream()
                                .map(SupplierCertificationDTO::from).toList()
                        : List.of()
        );
    }

    private ReportDetailDTO.Emissions emissions(Long supplierId, LocalDate start, LocalDate end) {
        return new ReportDetailDTO.Emissions(
                carbonEmissionRepository.sumByStage(supplierId, start, end).stream()
                        .map(row -> new ReportDetailDTO.ByStage(row.stageType(), row.co2Kg())).toList(),
                carbonEmissionRepository.sumByBatch(supplierId, start, end).stream()
                        .map(row -> new ReportDetailDTO.ByBatch(
                                row.batchId(), row.productName(), row.quantity(), row.unit(), row.co2Kg())).toList(),
                carbonEmissionRepository.sumByMethod(supplierId, start, end).stream()
                        .map(row -> new ReportDetailDTO.ByMethod(
                                row.calculationMethod(), row.emissionFactor(), row.co2Kg())).toList()
        );
    }

    private static ReportDetailDTO.Emissions emptyEmissions() {
        return new ReportDetailDTO.Emissions(List.of(), List.of(), List.of());
    }
}
