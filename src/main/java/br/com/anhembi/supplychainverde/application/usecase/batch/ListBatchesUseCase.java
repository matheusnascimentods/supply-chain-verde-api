package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchPageResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
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

    public BatchPageResponseDTO execute(int page, int size, Long supplierId) {
        var batches = supplierId == null
                ? batchRepository.findAll(page, size)
                : batchRepository.findBySupplierId(supplierId, page, size);
        long totalElements = supplierId == null
                ? batchRepository.countAll()
                : batchRepository.countBySupplierId(supplierId);
        int totalPages = PageCount.totalPages(totalElements, size);

        Map<Long, List<ChainResponseDTO>> stagesByBatch = stagesReader.findByBatchIds(
                batches.stream().map(batch -> batch.getBatchId()).toList());
        return new BatchPageResponseDTO(
                batches.stream().map(batch -> {
                    var batchDto = mapper.toDto(batch);
                    var stages = stagesByBatch.getOrDefault(batch.getBatchId(), List.of());
                    var currentStage = stages.isEmpty() ? null : stages.get(stages.size() - 1).stageType();
                    return new BatchResponseDTO(
                            batchDto.batchId(), batchDto.productId(), batchDto.productName(), batchDto.supplierId(),
                            batchDto.supplierName(), batchDto.quantity(), batchDto.producedAt(), currentStage, stages);
                }).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
