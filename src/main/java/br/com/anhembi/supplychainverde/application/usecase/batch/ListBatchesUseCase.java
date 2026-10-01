package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchPageResponseDTO;
import br.com.anhembi.supplychainverde.application.PageCount;
import br.com.anhembi.supplychainverde.application.mapper.BatchDtoMapper;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListBatchesUseCase {
    private final BatchRepository batchRepository;
    private final BatchDtoMapper mapper;

    public BatchPageResponseDTO execute(int page, int size, Long supplierId) {
        var batches = supplierId == null
                ? batchRepository.findAll(page, size)
                : batchRepository.findBySupplierId(supplierId, page, size);
        long totalElements = supplierId == null
                ? batchRepository.countAll()
                : batchRepository.countBySupplierId(supplierId);
        int totalPages = PageCount.totalPages(totalElements, size);

        return new BatchPageResponseDTO(
                batches.stream().map(mapper::toDto).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
