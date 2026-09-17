package br.com.anhembi.supplychainverde.domain.service;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SustainabilityScoreCalculatorTest {

    private final SustainabilityScoreCalculator calculator = new SustainabilityScoreCalculator();

    @Test
    void shouldAddBonusOnlyForActiveCertifications() {
        Certification active = Certification.builder().status(CertificationStatus.ACTIVE).build();
        Certification expired = Certification.builder().status(CertificationStatus.EXPIRED).build();

        assertThat(calculator.calculate(List.of(active, expired), List.of()))
                .isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(calculator.countActiveCertifications(List.of(active, expired))).isEqualTo(1);
    }

    @Test
    void shouldApplyCo2PenaltyAndClampNegativeScoresToZero() {
        CarbonEmission emissions = CarbonEmission.builder().co2Kg(new BigDecimal("250.00")).build();

        assertThat(calculator.calculate(List.of(), List.of(emissions)))
                .isEqualByComparingTo(new BigDecimal("97.50"));

        CarbonEmission veryHighEmission = CarbonEmission.builder().co2Kg(new BigDecimal("20000.00")).build();
        assertThat(calculator.calculate(List.of(), List.of(veryHighEmission)))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
