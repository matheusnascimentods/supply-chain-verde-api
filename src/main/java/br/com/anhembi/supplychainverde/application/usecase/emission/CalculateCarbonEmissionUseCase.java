package br.com.anhembi.supplychainverde.application.usecase.emission;

import org.springframework.security.access.AccessDeniedException;
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
import br.com.anhembi.supplychainverde.application.mapper.CarbonEmissionDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CalculateCarbonEmissionUseCase {
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final ChainRepository chainRepository;
    private final TransportRepository transportRepository;
    private final CarbonEmissionDtoMapper mapper;

    @Transactional
    /** RN-15: só o mercado ou o fornecedor do lote da etapa; os dois nulos são o PLATFORM_ADMIN. */
    public CarbonEmissionResponseDTO execute(Long chainId, CarbonEmissionRequestDTO request, Long marketId, Long supplierId) {
        if (request == null) throw new ValidationException("Requisição de emissão é obrigatória.");
        Chain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada: " + chainId));
        if (!chain.getBatch().isWithin(marketId, supplierId)) throw new AccessDeniedException("Lote de outra companhia.");
        Transport transport = transportRepository.findByChainId(chainId).orElse(null);
        BigDecimal distance = transport != null ? transport.getDistance() : BigDecimal.ZERO;
        BigDecimal emissionFactorValue = new BigDecimal("0.15");
        BigDecimal co2Kg = distance.multiply(emissionFactorValue);

        CarbonEmission emission = CarbonEmission.builder()
                .chain(chain)
                .emissionFactor(new EmissionFactor(emissionFactorValue))
                .co2Kg(co2Kg)
                .calculationMethod(request.calculationMethod())
                .build();
        return mapper.toResponse(carbonEmissionRepository.save(emission));
    }
}
