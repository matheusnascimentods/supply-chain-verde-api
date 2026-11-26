package br.com.anhembi.supplychainverde.application.dto.carbonemission;
import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.EnergySource;

import java.math.BigDecimal;

public record CarbonEmissionRequestDTO(CalculationMethod calculationMethod, BigDecimal cargoWeightKg,
                                       EnergySource energySource, BigDecimal consumption) {}
