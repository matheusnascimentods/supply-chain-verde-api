package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.report.*;
import br.com.anhembi.supplychainverde.application.usecase.report.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {
    private final GenerateSustainabilityReportUseCase generate;
    private final GetReportUseCase get;
    private final ListReportsBySupplierUseCase list;

    @PostMapping("/suppliers/{supplierId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponseDTO generate(@PathVariable Long supplierId, @RequestBody ReportRequestDTO request) { return generate.execute(supplierId, request); }

    @GetMapping("/reports/{reportId}")
    public ReportResponseDTO get(@PathVariable Long reportId) { return get.execute(reportId); }

    @GetMapping("/suppliers/{supplierId}/reports")
    public List<ReportResponseDTO> list(@PathVariable Long supplierId) { return list.execute(supplierId); }
}
