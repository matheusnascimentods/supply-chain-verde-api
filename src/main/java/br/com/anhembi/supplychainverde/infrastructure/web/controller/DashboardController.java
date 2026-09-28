package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.dashboard.DashboardSummaryResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.dashboard.GetDashboardSummaryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dashboard", description = "Resumo global dos indicadores da cadeia de suprimentos.")
public class DashboardController {
    private final GetDashboardSummaryUseCase getDashboardSummary;

    @GetMapping("/summary")
    @Operation(
            summary = "Consultar resumo do dashboard",
            description = "Retorna indicadores globais e os lotes recentes. Usuários de todos os perfis autenticados recebem o mesmo resumo."
    )
    public DashboardSummaryResponseDTO summary(
            @RequestParam(required = false) @Min(1) @Max(100) Integer limit
    ) {
        return getDashboardSummary.execute(limit);
    }
}
