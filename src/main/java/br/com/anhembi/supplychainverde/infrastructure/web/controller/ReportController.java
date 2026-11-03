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
    @Operation(summary = "Gerar relatório de sustentabilidade", description = "Somente mercado. Consolida as emissões dos lotes do mercado logado com o fornecedor no período (RN-23). Fornecedor ou PLATFORM_ADMIN recebem 403.")
    public ReportResponseDTO generate(@PathVariable Long supplierId, @RequestBody ReportRequestDTO request,
                                      @AuthenticationPrincipal CustomUserPrincipal principal) {
        return generate.execute(supplierId, principal.marketId(), request);
    }

    @GetMapping("/reports")
    @Operation(
            summary = "Consultar relatórios",
            description = "Sem reportId, lista relatórios paginados. Com reportId, retorna o detalhe do relatório para o PDF: dados do fornecedor, emissões do período por etapa, lote e metodologia, e certificações vigentes no período. O mercado vê só os próprios relatórios e o fornecedor só os relatórios sobre ele; fora disso, 403."
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
            return get.execute(reportId, principal.marketId(), principal.supplierId());
        }
        Long effectiveSupplierId = principal.isSupplierCompany() ? principal.supplierId() : supplierId;
        return listAll.execute(effectiveSupplierId, principal.marketId(), limit, offset);
    }
}
