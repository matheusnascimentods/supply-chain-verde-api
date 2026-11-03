package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.dashboard.DashboardSummaryResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.dashboard.MonthlyEmissionDTO;
import br.com.anhembi.supplychainverde.application.usecase.dashboard.GetDashboardSummaryUseCase;
import br.com.anhembi.supplychainverde.application.usecase.dashboard.GetMonthlyEmissionsUseCase;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dashboard", description = "Indicadores da cadeia de suprimentos: resumo global e série mensal de emissões.")
public class DashboardController {
    private final GetDashboardSummaryUseCase getDashboardSummary;
    private final GetMonthlyEmissionsUseCase getMonthlyEmissions;

    @GetMapping("/summary")
    @Operation(
            summary = "Consultar resumo do dashboard",
            description = "Retorna os indicadores e os lotes recentes da companhia do usuário. Para o mercado, suppliers conta os fornecedores com quem ele já trabalhou e pendingBatches vem nulo; para a companhia fornecedora, pendingBatches conta os lotes PENDING dela e suppliers vem nulo. O PLATFORM_ADMIN vê a plataforma inteira."
    )
    public DashboardSummaryResponseDTO summary(
            @RequestParam(required = false) @Min(1) @Max(100) Integer limit,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return getDashboardSummary.execute(limit, principal.marketId(), principal.supplierId());
    }

    @GetMapping("/emissions")
    @Operation(
            summary = "Consultar emissões mensais",
            description = "Retorna o CO₂ (kg) mês a mês entre from e to (formato YYYY-MM), incluindo meses sem emissão com co2Kg = 0. Sem parâmetros, retorna os últimos 12 meses até o atual. from posterior a to ou intervalo maior que 24 meses retornam 400. O mercado recebe as emissões dos próprios lotes e a companhia fornecedora as dela; o PLATFORM_ADMIN, a plataforma inteira."
    )
    public List<MonthlyEmissionDTO> emissions(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return getMonthlyEmissions.execute(from, to, principal.marketId(), principal.supplierId());
    }
}
