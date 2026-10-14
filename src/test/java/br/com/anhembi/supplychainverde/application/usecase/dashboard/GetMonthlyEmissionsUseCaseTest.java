package br.com.anhembi.supplychainverde.application.usecase.dashboard;

import br.com.anhembi.supplychainverde.application.dto.dashboard.MonthlyEmissionDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository.MonthlyEmission;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetMonthlyEmissionsUseCaseTest {
    private final CarbonEmissionRepository emissions = mock(CarbonEmissionRepository.class);
    private final GetMonthlyEmissionsUseCase useCase = new GetMonthlyEmissionsUseCase(emissions);

    @Test
    void shouldFillMonthsWithoutEmissionWithZero() {
        YearMonth january = YearMonth.of(2026, 1);
        YearMonth march = YearMonth.of(2026, 3);
        when(emissions.sumMonthly(january, march, 7L)).thenReturn(List.of(
                new MonthlyEmission(january, new BigDecimal("1250.40")),
                new MonthlyEmission(march, new BigDecimal("980.10"))));

        assertThat(useCase.execute(january, march, 7L)).containsExactly(
                new MonthlyEmissionDTO("2026-01", new BigDecimal("1250.40")),
                new MonthlyEmissionDTO("2026-02", BigDecimal.ZERO),
                new MonthlyEmissionDTO("2026-03", new BigDecimal("980.10")));
    }

    @Test
    void shouldReturnLastTwelveMonthsEndingInCurrentMonthWithoutParameters() {
        YearMonth now = YearMonth.now();

        List<MonthlyEmissionDTO> series = useCase.execute(null, null, null);

        assertThat(series).hasSize(12);
        assertThat(series.getLast().month()).isEqualTo(now.toString());
        verify(emissions).sumMonthly(now.minusMonths(11), now, null);
    }

    @Test
    void shouldEndTwelveMonthSeriesAtRequestedMonthWhenOnlyToIsSent() {
        List<MonthlyEmissionDTO> series = useCase.execute(null, YearMonth.of(2026, 6), null);

        assertThat(series).hasSize(12);
        assertThat(series.getFirst().month()).isEqualTo("2025-07");
        assertThat(series.getLast().month()).isEqualTo("2026-06");
    }

    @Test
    void shouldAcceptTwentyFourMonths() {
        assertThat(useCase.execute(YearMonth.of(2025, 1), YearMonth.of(2026, 12), null)).hasSize(24);
    }

    @Test
    void shouldRejectMoreThanTwentyFourMonths() {
        assertThatThrownBy(() -> useCase.execute(YearMonth.of(2025, 1), YearMonth.of(2027, 1), null))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(emissions);
    }

    @Test
    void shouldRejectInvertedRange() {
        assertThatThrownBy(() -> useCase.execute(YearMonth.of(2026, 6), YearMonth.of(2026, 1), null))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(emissions);
    }
}
