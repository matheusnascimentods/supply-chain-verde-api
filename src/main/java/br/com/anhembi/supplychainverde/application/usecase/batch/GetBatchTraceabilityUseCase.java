package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchTraceabilityResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GetBatchTraceabilityUseCase {
    private final BatchRepository batchRepository;
    private final BatchStagesReader stagesReader;

    public BatchTraceabilityResponseDTO execute(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Lote não encontrado: " + batchId));

        List<ChainResponseDTO> stages = stagesReader
                .findByBatchIds(List.of(batchId)).getOrDefault(batchId, List.of());
        BigDecimal totalCo2Kg = stages.stream()
                .map(ChainResponseDTO::emission)
                .filter(Objects::nonNull)
                .map(emission -> emission.co2Kg())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BatchTraceabilityResponseDTO(
                batch.getBatchId(),
                batch.getProduct() != null ? batch.getProduct().getName() : null,
                batch.getSupplier() != null ? batch.getSupplier().getName() : null,
                batch.getQuantity(),
                batch.getProducedAt(),
                stages,
                totalCo2Kg
        );
    }
}
