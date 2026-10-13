package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.report.ReportDetailDTO;
import br.com.anhembi.supplychainverde.application.usecase.report.GenerateSustainabilityReportUseCase;
import br.com.anhembi.supplychainverde.application.usecase.report.GetReportUseCase;
import br.com.anhembi.supplychainverde.application.usecase.report.ListReportsUseCase;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportControllerTest {
    private final GetReportUseCase getReport = mock(GetReportUseCase.class);
    private final ListReportsUseCase listReports = mock(ListReportsUseCase.class);
    private final ReportController controller = new ReportController(
            mock(GenerateSustainabilityReportUseCase.class), getReport, listReports);
    private final CustomUserPrincipal supplier = new CustomUserPrincipal(3L, "s@example.com", "SUPPLIER", 7L);

    @Test
    void shouldListOnlyLinkedSupplierReportsForSupplier() {
        controller.listAll(null, 99L, 20, 0, supplier);

        verify(listReports).execute(7L, 20, 0);
    }

    @Test
    void shouldReturnReportOfLinkedSupplier() {
        ReportDetailDTO report = reportOf(7L);
        when(getReport.execute(1L)).thenReturn(report);

        assertThat(controller.listAll(1L, null, 20, 0, supplier)).isEqualTo(report);
    }

    @Test
    void shouldDenyReportOfAnotherSupplier() {
        when(getReport.execute(1L)).thenReturn(reportOf(3L));

        assertThatThrownBy(() -> controller.listAll(1L, null, 20, 0, supplier))
                .isInstanceOf(AccessDeniedException.class);
    }

    private ReportDetailDTO reportOf(Long supplierId) {
        return new ReportDetailDTO(1L, supplierId, null, null, null, null, null, 0, null, null);
    }
}
