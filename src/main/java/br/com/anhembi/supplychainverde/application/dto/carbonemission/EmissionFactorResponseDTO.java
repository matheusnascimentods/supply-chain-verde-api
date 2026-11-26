package br.com.anhembi.supplychainverde.application.dto.carbonemission;

import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.EnergySource;
import br.com.anhembi.supplychainverde.domain.enums.FuelType;
import br.com.anhembi.supplychainverde.domain.enums.TransportMode;

import java.math.BigDecimal;

public record EmissionFactorResponseDTO(
        CalculationMethod calculationMethod,
        TransportMode transportMode,
        FuelType fuelType,
        EnergySource energySource,
        BigDecimal value,
        String unit,
        String source,
        int referenceYear
) {}
