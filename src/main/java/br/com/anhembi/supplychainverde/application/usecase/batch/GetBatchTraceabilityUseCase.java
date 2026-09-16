package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchTraceabilityResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBatchTraceabilityUseCase {
    private final BatchRepository batchRepository;
    private final ChainRepository chainRepository;

    public BatchTraceabilityResponseDTO execute(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Lote não encontrado: " + batchId));

        List<ChainResponseDTO> stages = chainRepository.findByBatchId(batchId).stream()
                .map(chain -> new ChainResponseDTO(
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
                )).toList();

        return new BatchTraceabilityResponseDTO(
                batch.getBatchId(),
                batch.getProduct() != null ? batch.getProduct().getName() : null,
                batch.getSupplier() != null ? batch.getSupplier().getName() : null,
                batch.getQuantity(),
                batch.getProducedAt(),
                stages,
                BigDecimal.ZERO
        );
    }
}
