package br.com.anhembi.supplychainverde.application.usecase.chain;

import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.application.mapper.ChainDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListChainStagesByBatchUseCase {
    private final ChainRepository chainRepository;
    private final ChainDtoMapper mapper;

    public List<ChainResponseDTO> execute(Long batchId) {
        return chainRepository.findByBatchId(batchId).stream().map(chain -> mapper.toResponse(chain, null, null)).toList();
    }
}
