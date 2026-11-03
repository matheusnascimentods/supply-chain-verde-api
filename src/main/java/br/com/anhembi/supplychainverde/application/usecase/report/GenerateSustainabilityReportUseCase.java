package br.com.anhembi.supplychainverde.application.usecase.report;

import br.com.anhembi.supplychainverde.application.dto.report.ReportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.report.ReportResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.application.mapper.ReportDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenerateSustainabilityReportUseCase {
    private final ReportRepository reportRepository;
    private final CompanyRepository supplierRepository;
    private final ReportDtoMapper mapper;

    @Transactional
    /** RN-23: o relatório é do mercado que o gera; marketId nulo (fornecedor ou PLATFORM_ADMIN) não gera. */
    public ReportResponseDTO execute(Long supplierId, Long marketId, ReportRequestDTO request) {
        if (marketId == null) throw new AccessDeniedException("Só um mercado gera relatórios.");
        if (request == null || request.periodStartAt() == null || request.periodEndAt() == null) {
            throw new ValidationException("Período do relatório é obrigatório.");
        }
        if (request.periodEndAt().isBefore(request.periodStartAt())) {
            throw new ValidationException("A data final do período deve ser igual ou posterior à inicial.");
        }
        supplierRepository.findSupplierById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + supplierId));

        // Os totais são consolidados pela sp_generate_sustainability_report (V36).
        Long reportId = reportRepository.generate(supplierId, marketId, request.periodStartAt(), request.periodEndAt());
        return mapper.toResponse(reportRepository.findById(reportId).orElseThrow());
    }
}
