package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.EnergySource;
import br.com.anhembi.supplychainverde.domain.enums.FuelType;
import br.com.anhembi.supplychainverde.domain.enums.TransportMode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EmissionFactorRepository {
    List<Entry> findAll();

    Optional<Entry> findTransport(CalculationMethod method, TransportMode mode, FuelType fuel);

    Optional<Entry> findEnergy(CalculationMethod method, EnergySource source);

    record Entry(CalculationMethod calculationMethod, TransportMode transportMode, FuelType fuelType,
                 EnergySource energySource, BigDecimal value, String unit, String source, int referenceYear) {}
}
