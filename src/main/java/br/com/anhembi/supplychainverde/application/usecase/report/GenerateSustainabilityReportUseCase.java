package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.report.ReportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.report.ReportResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.application.mapper.ReportDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenerateSustainabilityReportUseCase {
    private final ReportRepository reportRepository;
    private final SupplierRepository supplierRepository;
    private final ReportDtoMapper mapper;

    @Transactional
    public ReportResponseDTO execute(Long supplierId, ReportRequestDTO request) {
        if (request == null || request.periodStartAt() == null || request.periodEndAt() == null) {
            throw new ValidationException("Período do relatório é obrigatório.");
        }
        if (request.periodEndAt().isBefore(request.periodStartAt())) {
            throw new ValidationException("A data final do período deve ser igual ou posterior à inicial.");
        }
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + supplierId));

        // Os totais são consolidados pela sp_generate_sustainability_report (V36).
        Long reportId = reportRepository.generate(supplierId, request.periodStartAt(), request.periodEndAt());
        return mapper.toResponse(reportRepository.findById(reportId).orElseThrow());
    }
}
