package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListBatchesBySupplierUseCase {
    private final BatchRepository batchRepository;

    public List<BatchResponseDTO> execute(Long supplierId) {
        return batchRepository.findBySupplierId(supplierId).stream().map(this::toResponse).toList();
    }

    private BatchResponseDTO toResponse(Batch batch) {
        return new BatchResponseDTO(
                batch.getBatchId(),
                batch.getProduct() != null ? batch.getProduct().getProductId() : null,
                batch.getProduct() != null ? batch.getProduct().getName() : null,
                batch.getSupplier() != null ? batch.getSupplier().getSupplierId() : null,
                batch.getSupplier() != null ? batch.getSupplier().getName() : null,
                batch.getQuantity(),
                batch.getProducedAt()
        );
    }
}
