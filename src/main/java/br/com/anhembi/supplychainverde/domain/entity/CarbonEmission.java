package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.valueobject.EmissionFactor;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonEmission {
    private Long emissionId;
    private Chain chain;
    private EmissionFactor emissionFactor;
    private BigDecimal co2Kg;
    private CalculationMethod calculationMethod;
    private LocalDate calculatedAt;
}
