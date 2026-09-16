package br.com.anhembi.supplychainverde.application.usecase.chain;

import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListChainStagesByBatchUseCase {
    private final ChainRepository chainRepository;

    public List<ChainResponseDTO> execute(Long batchId) {
        return chainRepository.findByBatchId(batchId).stream().map(this::toResponse).toList();
    }

    private ChainResponseDTO toResponse(Chain chain) {
        return new ChainResponseDTO(
                chain.getChainId(),
                chain.getBatch() != null ? chain.getBatch().getBatchId() : null,
                null,
                null,
                chain.getResponsibleUser() != null ? chain.getResponsibleUser().getUserId() : null,
                chain.getResponsibleUser() != null ? chain.getResponsibleUser().getName() : null,
                chain.getStageType(),
                chain.getStartedAt(),
                chain.getEndedAt(),
                null,
                null
        );
    }
}
