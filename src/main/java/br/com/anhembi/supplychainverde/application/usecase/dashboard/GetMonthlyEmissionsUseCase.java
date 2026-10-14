package br.com.anhembi.supplychainverde.application.usecase.dashboard;

import br.com.anhembi.supplychainverde.application.dto.dashboard.MonthlyEmissionDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository.MonthlyEmission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetMonthlyEmissionsUseCase {
    private static final int DEFAULT_MONTHS = 12;
    private static final int MAX_MONTHS = 24;

    private final CarbonEmissionRepository carbonEmissionRepository;

    public List<MonthlyEmissionDTO> execute(YearMonth from, YearMonth to, Long supplierId) {
        YearMonth end = to != null ? to : YearMonth.now();
        YearMonth start = from != null ? from : end.minusMonths(DEFAULT_MONTHS - 1);

        if (start.isAfter(end)) {
            throw new ValidationException("from não pode ser posterior a to.");
        }
        if (start.until(end, ChronoUnit.MONTHS) + 1 > MAX_MONTHS) {
            throw new ValidationException("O intervalo máximo é de " + MAX_MONTHS + " meses.");
        }

        Map<YearMonth, BigDecimal> co2ByMonth = carbonEmissionRepository.sumMonthly(start, end, supplierId)
                .stream()
                .collect(
                    Collectors.toMap(
                            MonthlyEmission::month,
                            MonthlyEmission::co2Kg
                    )
                );
        List<MonthlyEmissionDTO> series = new ArrayList<>();
        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            series.add(new MonthlyEmissionDTO(month.toString(), co2ByMonth.getOrDefault(month, BigDecimal.ZERO)));
        }

        return series;
    }
}
