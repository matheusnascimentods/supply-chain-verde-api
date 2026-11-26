package br.com.anhembi.supplychainverde.application.usecase.emission;

import org.springframework.security.access.AccessDeniedException;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.EmissionFactorRepository;
import br.com.anhembi.supplychainverde.domain.repository.TransportRepository;
import br.com.anhembi.supplychainverde.domain.valueobject.EmissionFactor;
import br.com.anhembi.supplychainverde.application.mapper.CarbonEmissionDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CalculateCarbonEmissionUseCase {
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final ChainRepository chainRepository;
    private final TransportRepository transportRepository;
    private final EmissionFactorRepository emissionFactorRepository;
    private final CarbonEmissionDtoMapper mapper;

    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    @Transactional
    /** RN-15: só o mercado ou o fornecedor do lote da etapa; os dois nulos são o PLATFORM_ADMIN. */
    public CarbonEmissionResponseDTO execute(Long chainId, CarbonEmissionRequestDTO request, Long marketId, Long supplierId) {
        if (request == null) throw new ValidationException("Requisição de emissão é obrigatória.");
        Chain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Etapa não encontrada: " + chainId));
        if (!chain.getBatch().isWithin(marketId, supplierId)) throw new AccessDeniedException("Lote de outra companhia.");
        if (carbonEmissionRepository.findByChainId(chainId).isPresent()) {
            throw new ConflictException("A emissão da etapa " + chainId + " já foi calculada.");
        }
        if (request.calculationMethod() == null) {
            throw new ValidationException("Metodologia é obrigatória.");
        }

        CarbonEmission.CarbonEmissionBuilder emission = CarbonEmission.builder()
                .chain(chain)
                .calculationMethod(request.calculationMethod());
        BigDecimal factor;
        BigDecimal co2Kg;

        if (chain.getStageType().isTransport()) {
            Transport transport = transportRepository.findByChainId(chainId)
                    .orElseThrow(() -> new ValidationException("Registre o transporte da etapa antes de calcular a emissão."));
            BigDecimal weightKg = cargoWeightKg(chain, request.cargoWeightKg());
            factor = emissionFactorRepository.findTransport(
                            request.calculationMethod(), transport.getTransportMode(), transport.getFuelType())
                    .orElseThrow(() -> new ValidationException(missingFactorMessage(
                            request.calculationMethod(), transport)))
                    .value();
            co2Kg = transport.getDistance()
                    .multiply(weightKg)
                    .multiply(factor)
                    .divide(THOUSAND);
            emission.cargoWeightKg(weightKg);
        } else {
            if (request.energySource() == null) {
                throw new ValidationException("Fonte de energia é obrigatória.");
            }
            if (request.consumption() == null) {
                throw new ValidationException("Consumo é obrigatório.");
            }
            if (request.consumption().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Consumo não pode ser negativo.");
            }
            factor = emissionFactorRepository.findEnergy(request.calculationMethod(), request.energySource())
                    .orElseThrow(() -> new ValidationException(missingFixedFactorMessage(
                            request.calculationMethod(), request.energySource())))
                    .value();
            co2Kg = request.consumption().multiply(factor);
            emission.energySource(request.energySource()).consumption(request.consumption());
        }

        emission.emissionFactor(new EmissionFactor(factor))
                .co2Kg(co2Kg.setScale(4, RoundingMode.HALF_UP));
        return mapper.toResponse(carbonEmissionRepository.save(emission.build()));
    }

    private BigDecimal cargoWeightKg(Chain chain, BigDecimal informedWeightKg) {
        ProductUnit unit = chain.getBatch().getProduct().getUnit();
        if (unit == ProductUnit.KG) {
            return chain.getBatch().getQuantity();
        }
        if (unit == ProductUnit.TON) {
            return chain.getBatch().getQuantity().multiply(THOUSAND);
        }
        if (informedWeightKg == null || informedWeightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Peso da carga é obrigatório e maior que zero.");
        }
        return informedWeightKg;
    }

    private String missingFactorMessage(br.com.anhembi.supplychainverde.domain.enums.CalculationMethod method,
                                        Transport transport) {
        return "A metodologia " + method + " não tem fator para "
                + transportModeLabel(transport.getTransportMode()) + " + "
                + fuelLabel(transport.getFuelType()) + ".";
    }

    private String missingFixedFactorMessage(br.com.anhembi.supplychainverde.domain.enums.CalculationMethod method,
                                             br.com.anhembi.supplychainverde.domain.enums.EnergySource source) {
        return "A metodologia " + method + " não tem fator para " + energyLabel(source) + ".";
    }

    private String transportModeLabel(br.com.anhembi.supplychainverde.domain.enums.TransportMode mode) {
        return switch (mode) {
            case ROAD -> "Rodoviário";
            case RAIL -> "Ferroviário";
            case MARITIME -> "Marítimo";
            case AIR -> "Aéreo";
        };
    }

    private String fuelLabel(br.com.anhembi.supplychainverde.domain.enums.FuelType fuel) {
        return switch (fuel) {
            case DIESEL -> "Diesel";
            case BIODIESEL -> "Biodiesel";
            case ELECTRIC -> "Elétrico";
            case GASOLINE -> "Gasolina";
            case ETHANOL -> "Etanol";
            case AVIATION_KEROSENE -> "Querosene de aviação";
            case HEAVY_FUEL_OIL -> "Óleo combustível";
        };
    }

    private String energyLabel(br.com.anhembi.supplychainverde.domain.enums.EnergySource source) {
        return switch (source) {
            case ELECTRICITY -> "Eletricidade";
            case DIESEL -> "Diesel";
            case LPG -> "GLP";
            case NATURAL_GAS -> "Gás natural";
        };
    }
}
