package br.com.anhembi.supplychainverde.application.usecase.emission;

import br.com.anhembi.supplychainverde.application.dto.carbonemission.EmissionFactorResponseDTO;
import br.com.anhembi.supplychainverde.domain.repository.EmissionFactorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListEmissionFactorsUseCase {
    private final EmissionFactorRepository emissionFactorRepository;

    public List<EmissionFactorResponseDTO> execute() {
        return emissionFactorRepository.findAll().stream()
                .map(entry -> new EmissionFactorResponseDTO(
                        entry.calculationMethod(),
                        entry.transportMode(),
                        entry.fuelType(),
                        entry.energySource(),
                        entry.value(),
                        entry.unit(),
                        entry.source(),
                        entry.referenceYear()
                ))
                .toList();
    }
}
