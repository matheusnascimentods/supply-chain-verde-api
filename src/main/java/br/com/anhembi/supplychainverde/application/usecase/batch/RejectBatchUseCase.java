package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.RejectBatchRequestDTO;
import br.com.anhembi.supplychainverde.application.mapper.BatchDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.exception.BatchNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RejectBatchUseCase {
    private final BatchRepository batchRepository;
    private final BatchDtoMapper mapper;

    /** RN-19: só o fornecedor do lote recusa, e só enquanto PENDING (409 depois). */
    @Transactional
    public BatchResponseDTO execute(Long batchId, String reason, Long supplierId) {
        Batch batch = batchRepository.findById(batchId).orElseThrow(() -> new BatchNotFoundException(batchId));
        if (supplierId == null || !batch.isWithin(null, supplierId)) {
            throw new AccessDeniedException("Só o fornecedor do lote o recusa.");
        }
        batch.reject(reason);
        return mapper.toResponse(batchRepository.save(batch), null, List.of());
    }
}
