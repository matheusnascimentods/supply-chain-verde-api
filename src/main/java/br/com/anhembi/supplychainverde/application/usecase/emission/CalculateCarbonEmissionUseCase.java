package br.com.anhembi.supplychainverde.application.usecase.emission;

import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.TransportRepository;
import br.com.anhembi.supplychainverde.domain.valueobject.EmissionFactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CalculateCarbonEmissionUseCase {
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final ChainRepository chainRepository;
    private final TransportRepository transportRepository;

    public CarbonEmissionResponseDTO execute(Long chainId, CarbonEmissionRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de emissão é obrigatória.");
        Chain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada: " + chainId));
        Transport transport = transportRepository.findByChainId(chainId).orElse(null);
        BigDecimal distance = transport != null ? transport.getDistance() : BigDecimal.ZERO;
        BigDecimal emissionFactorValue = new BigDecimal("0.15");
        BigDecimal co2Kg = distance.multiply(emissionFactorValue);

        CarbonEmission emission = CarbonEmission.builder()
                .chain(chain)
                .emissionFactor(new EmissionFactor(emissionFactorValue))
                .co2Kg(co2Kg)
                .calculationMethod(request.calculationMethod())
                .calculatedAt(LocalDate.now())
                .build();
        CarbonEmission saved = carbonEmissionRepository.save(emission);
        return new CarbonEmissionResponseDTO(saved.getEmissionId(), chainId, saved.getEmissionFactor().value(), saved.getCo2Kg(), saved.getCalculationMethod(), saved.getCalculatedAt());
    }
}
