package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.TransportRepository;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchStagesReader {
    private final ChainRepository chainRepository;
    private final TransportRepository transportRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;

    public Map<Long, List<ChainResponseDTO>> findByBatchIds(Collection<Long> batchIds) {
        if (batchIds.isEmpty()) return Map.of();

        List<Chain> chains = chainRepository.findByBatchIds(batchIds).stream()
                .sorted(Comparator.comparing(Chain::getStartedAt).thenComparing(Chain::getChainId))
                .toList();
        if (chains.isEmpty()) return Map.of();

        Set<Long> chainIds = chains.stream().map(Chain::getChainId).collect(Collectors.toSet());
        Map<Long, Transport> transports = transportRepository.findByChainIds(chainIds).stream()
                .collect(Collectors.toMap(t -> t.getChain().getChainId(), Function.identity()));
        Map<Long, CarbonEmission> emissions = carbonEmissionRepository.findByChainIds(chainIds).stream()
                .collect(Collectors.toMap(e -> e.getChain().getChainId(), Function.identity()));

        return chains.stream().map(chain -> toResponse(chain, transports.get(chain.getChainId()), emissions.get(chain.getChainId())))
                .collect(Collectors.groupingBy(ChainResponseDTO::batchId, Collectors.toList()));
    }

    private ChainResponseDTO toResponse(Chain chain, Transport transport, CarbonEmission emission) {
        return new ChainResponseDTO(
                chain.getChainId(),
                chain.getBatch() == null ? null : chain.getBatch().getBatchId(),
                toAddressResponse(chain.getOriginAddress()),
                toAddressResponse(chain.getDestinationAddress()),
                chain.getResponsibleUser() == null ? null : chain.getResponsibleUser().getUserId(),
                chain.getResponsibleUser() == null ? null : chain.getResponsibleUser().getName(),
                chain.getStageType(),
                chain.getStartedAt(),
                chain.getEndedAt(),
                transport == null ? null : new TransportResponseDTO(
                        transport.getTransportId(), chain.getChainId(), transport.getTransportMode(),
                        transport.getDistance(), transport.getFuelType(), transport.getCapacity()),
                emission == null ? null : new CarbonEmissionResponseDTO(
                        emission.getEmissionId(), chain.getChainId(),
                        emission.getEmissionFactor() == null ? null : emission.getEmissionFactor().value(),
                        emission.getCo2Kg(), emission.getCalculationMethod(), emission.getCalculatedAt())
        );
    }

    private AddressResponseDTO toAddressResponse(br.com.anhembi.supplychainverde.domain.entity.Address address) {
        if (address == null) return null;
        return new AddressResponseDTO(address.getAddressId(), address.getStreet(), address.getNumber(),
                address.getNeighborhood(), address.getComplement(), address.getZipCode(), address.getCity(), address.getState());
    }
}
