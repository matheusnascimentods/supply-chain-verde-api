package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.EnergySource;
import br.com.anhembi.supplychainverde.domain.enums.FuelType;
import br.com.anhembi.supplychainverde.domain.enums.TransportMode;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.EmissionFactorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmissionFactorJpaRepository extends JpaRepository<EmissionFactorJpaEntity, Long> {
    Optional<EmissionFactorJpaEntity> findByCalculationMethodAndTransportModeAndFuelType(
            CalculationMethod calculationMethod, TransportMode transportMode, FuelType fuelType);

    Optional<EmissionFactorJpaEntity> findByCalculationMethodAndEnergySource(
            CalculationMethod calculationMethod, EnergySource energySource);
}
