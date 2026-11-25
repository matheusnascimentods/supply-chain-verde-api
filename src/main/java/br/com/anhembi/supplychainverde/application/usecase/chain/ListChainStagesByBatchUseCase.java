package br.com.anhembi.supplychainverde.application.usecase.chain;

import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.application.mapper.ChainDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ListChainStagesByBatchUseCase {
    private final ChainRepository chainRepository;
    private final ChainDtoMapper mapper;

    public List<ChainResponseDTO> execute(Long batchId) {
        List<Chain> chains = chainRepository.findByBatchId(batchId);
        Map<Long, Address> origins = Chain.origins(chains);
        return chains.stream().map(chain -> mapper.toResponse(chain, origins.get(chain.getChainId()), null, null)).toList();
    }
}
