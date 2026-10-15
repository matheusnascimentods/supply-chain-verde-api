package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.TransportRepository;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.application.mapper.ChainDtoMapper;
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
    private final ChainDtoMapper mapper;

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

        return chains.stream().map(chain -> mapper.toResponse(chain, transports.get(chain.getChainId()), emissions.get(chain.getChainId())))
                .collect(Collectors.groupingBy(ChainResponseDTO::batchId, Collectors.toList()));
    }
}
