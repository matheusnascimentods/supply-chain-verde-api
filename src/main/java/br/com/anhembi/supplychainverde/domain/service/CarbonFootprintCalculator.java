package br.com.anhembi.supplychainverde.domain.service;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;

import java.math.BigDecimal;
import java.util.List;

public class CarbonFootprintCalculator {
    public BigDecimal calculateTotal(List<CarbonEmission> emissions) {
        if (emissions == null || emissions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return emissions.stream()
                .map(CarbonEmission::getCo2Kg)
                .filter(co2Kg -> co2Kg != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
