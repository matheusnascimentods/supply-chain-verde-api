package br.com.anhembi.supplychainverde.domain.service;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarbonFootprintCalculatorTest {

    private final CarbonFootprintCalculator calculator = new CarbonFootprintCalculator();

    @Test
    void shouldReturnZeroForNullOrEmptyEmissions() {
        assertThat(calculator.calculateTotal(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(calculator.calculateTotal(List.of())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldSumOnlyNonNullEmissionValues() {
        CarbonEmission first = CarbonEmission.builder().co2Kg(new BigDecimal("12.50")).build();
        CarbonEmission second = CarbonEmission.builder().co2Kg(null).build();
        CarbonEmission third = CarbonEmission.builder().co2Kg(new BigDecimal("7.50")).build();

        assertThat(calculator.calculateTotal(List.of(first, second, third)))
                .isEqualByComparingTo(new BigDecimal("20.00"));
    }
}
