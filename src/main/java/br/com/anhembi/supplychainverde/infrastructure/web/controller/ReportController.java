package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.report.*;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.report.*;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Relatórios", description = "Geração e consulta de relatórios de sustentabilidade.")
public class ReportController {
    private final GenerateSustainabilityReportUseCase generate;
    private final GetReportUseCase get;
    private final ListReportsUseCase listAll;

    @PostMapping("/suppliers/{supplierId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gerar relatório de sustentabilidade", description = "Consolida emissões e dados rastreados de um fornecedor em um período.")
    public ReportResponseDTO generate(@PathVariable Long supplierId, @RequestBody ReportRequestDTO request) { return generate.execute(supplierId, request); }

    @GetMapping("/reports")
    @Operation(
            summary = "Consultar relatórios",
            description = "Sem reportId, lista relatórios paginados. Com reportId, retorna o detalhe do relatório. SUPPLIER só consulta os próprios relatórios."
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(oneOf = {
            ReportDetailDTO.class,
            OffsetPageResponseDTO.class
    })))
    public Object listAll(
            @RequestParam(required = false) @Min(1) Long reportId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        if (reportId != null) {
            ReportDetailDTO report = get.execute(reportId);
            ensureSupplierOwns(principal, report.supplierId());
            return report;
        }
        Long effectiveSupplierId = isSupplier(principal) ? principal.userId() : supplierId;
        return listAll.execute(effectiveSupplierId, limit, offset);
    }

    private boolean isSupplier(CustomUserPrincipal principal) {
        return "SUPPLIER".equalsIgnoreCase(principal.role());
    }

    private void ensureSupplierOwns(CustomUserPrincipal principal, Long supplierId) {
        if (isSupplier(principal) && !principal.userId().equals(supplierId)) {
            throw new AccessDeniedException("Fornecedor pode consultar somente os próprios relatórios.");
        }
    }
}
