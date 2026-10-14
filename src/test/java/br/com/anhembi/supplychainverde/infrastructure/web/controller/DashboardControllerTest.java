package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.usecase.dashboard.GetDashboardSummaryUseCase;
import br.com.anhembi.supplychainverde.application.usecase.dashboard.GetMonthlyEmissionsUseCase;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DashboardControllerTest {
    private final GetMonthlyEmissionsUseCase monthlyEmissions = mock(GetMonthlyEmissionsUseCase.class);
    private final DashboardController controller =
            new DashboardController(mock(GetDashboardSummaryUseCase.class), monthlyEmissions);
    private final YearMonth from = YearMonth.of(2026, 1);
    private final YearMonth to = YearMonth.of(2026, 6);

    @Test
    void shouldScopeEmissionsToLinkedSupplierForSupplier() {
        controller.emissions(from, to, new CustomUserPrincipal(3L, "s@example.com", "SUPPLIER", 7L));

        verify(monthlyEmissions).execute(from, to, 7L);
    }

    @Test
    void shouldReturnWholeChainForNonSupplier() {
        controller.emissions(from, to, new CustomUserPrincipal(1L, "a@example.com", "ADMIN", null));

        verify(monthlyEmissions).execute(from, to, null);
    }
}
