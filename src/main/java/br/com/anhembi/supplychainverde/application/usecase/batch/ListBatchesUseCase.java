package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchPageResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.application.pagination.PageCount;
import br.com.anhembi.supplychainverde.application.mapper.BatchDtoMapper;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ListBatchesUseCase {
    private final BatchRepository batchRepository;
    private final BatchDtoMapper mapper;
    private final BatchStagesReader stagesReader;

    /** RN-12: marketId e supplierId nulos (PLATFORM_ADMIN sem filtro) listam todos os lotes. */
    public BatchPageResponseDTO execute(int page, int size, Long marketId, Long supplierId, BatchStatus status) {
        var batches = batchRepository.findPage(marketId, supplierId, status, page, size);
        long totalElements = batchRepository.countPage(marketId, supplierId, status);
        int totalPages = PageCount.totalPages(totalElements, size);

        Map<Long, List<ChainResponseDTO>> stagesByBatch = stagesReader.findByBatchIds(
                batches.stream().map(batch -> batch.getBatchId()).toList());
        return new BatchPageResponseDTO(
                batches.stream().map(batch -> {
                    var stages = stagesByBatch.getOrDefault(batch.getBatchId(), List.<ChainResponseDTO>of());
                    var currentStage = stages.isEmpty() ? null : stages.get(stages.size() - 1).stageType();
                    return mapper.toResponse(batch, currentStage, stages);
                }).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
