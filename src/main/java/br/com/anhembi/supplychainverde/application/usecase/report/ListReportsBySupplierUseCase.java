package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.report.ReportResponseDTO;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.application.mapper.ReportDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListReportsBySupplierUseCase {
    private final ReportRepository reportRepository;
    private final ReportDtoMapper mapper;

    public List<ReportResponseDTO> execute(Long supplierId) {
        return reportRepository.findBySupplierId(supplierId).stream().map(mapper::toResponse).toList();
    }
}
