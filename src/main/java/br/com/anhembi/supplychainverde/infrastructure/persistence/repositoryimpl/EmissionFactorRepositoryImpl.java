package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.EnergySource;
import br.com.anhembi.supplychainverde.domain.enums.FuelType;
import br.com.anhembi.supplychainverde.domain.enums.TransportMode;
import br.com.anhembi.supplychainverde.domain.repository.EmissionFactorRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.EmissionFactorJpaEntity;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.EmissionFactorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmissionFactorRepositoryImpl implements EmissionFactorRepository {
    private final EmissionFactorJpaRepository jpaRepository;

    @Override
    public List<Entry> findAll() {
        return jpaRepository.findAll().stream().map(this::toEntry).toList();
    }

    @Override
    public Optional<Entry> findTransport(CalculationMethod method, TransportMode mode, FuelType fuel) {
        return jpaRepository.findByCalculationMethodAndTransportModeAndFuelType(method, mode, fuel)
                .map(this::toEntry);
    }

    @Override
    public Optional<Entry> findEnergy(CalculationMethod method, EnergySource source) {
        return jpaRepository.findByCalculationMethodAndEnergySource(method, source).map(this::toEntry);
    }

    private Entry toEntry(EmissionFactorJpaEntity entity) {
        return new Entry(
                entity.getCalculationMethod(),
                entity.getTransportMode(),
                entity.getFuelType(),
                entity.getEnergySource(),
                entity.getValue(),
                entity.getUnit(),
                entity.getSource(),
                entity.getReferenceYear()
        );
    }
}
