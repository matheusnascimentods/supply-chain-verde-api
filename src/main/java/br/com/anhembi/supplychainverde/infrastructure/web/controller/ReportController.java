package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.report.*;
import br.com.anhembi.supplychainverde.application.usecase.report.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Geração e consulta de relatórios de sustentabilidade.")
public class ReportController {
    private final GenerateSustainabilityReportUseCase generate;
    private final GetReportUseCase get;
    private final ListReportsBySupplierUseCase list;

    @PostMapping("/suppliers/{supplierId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gerar relatório de sustentabilidade", description = "Consolida emissões e dados rastreados de um fornecedor em um período.")
    public ReportResponseDTO generate(@PathVariable Long supplierId, @RequestBody ReportRequestDTO request) { return generate.execute(supplierId, request); }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Consultar relatório", description = "Busca um relatório pelo identificador.")
    public ReportResponseDTO get(@PathVariable Long reportId) { return get.execute(reportId); }

    @GetMapping("/suppliers/{supplierId}/reports")
    @Operation(summary = "Listar relatórios do fornecedor", description = "Retorna os relatórios de sustentabilidade de um fornecedor.")
    public List<ReportResponseDTO> list(@PathVariable Long supplierId) { return list.execute(supplierId); }
}
