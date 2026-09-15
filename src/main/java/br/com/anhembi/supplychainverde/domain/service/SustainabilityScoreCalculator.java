package br.com.anhembi.supplychainverde.domain.service;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class SustainabilityScoreCalculator {
    private static final BigDecimal BASE_SCORE = BigDecimal.valueOf(100);
    private static final BigDecimal ACTIVE_CERTIFICATION_BONUS = BigDecimal.valueOf(10);
    private static final BigDecimal CO2_PENALTY_DIVISOR = BigDecimal.valueOf(100);

    public BigDecimal calculate(List<Certification> certifications, List<CarbonEmission> emissions) {
        BigDecimal score = BASE_SCORE
                .add(ACTIVE_CERTIFICATION_BONUS.multiply(BigDecimal.valueOf(countActiveCertifications(certifications))))
                .subtract(calculateCo2Penalty(emissions));

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    public int countActiveCertifications(List<Certification> certifications) {
        if (certifications == null || certifications.isEmpty()) {
            return 0;
        }

        return (int) certifications.stream()
                .filter(certification -> certification != null
                        && certification.getStatus() == CertificationStatus.ACTIVE)
                .count();
    }

    private BigDecimal calculateCo2Penalty(List<CarbonEmission> emissions) {
        if (emissions == null || emissions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCo2Kg = emissions.stream()
                .filter(emission -> emission != null && emission.getCo2Kg() != null)
                .map(CarbonEmission::getCo2Kg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalCo2Kg.divide(CO2_PENALTY_DIVISOR, 2, RoundingMode.HALF_UP);
    }
}
