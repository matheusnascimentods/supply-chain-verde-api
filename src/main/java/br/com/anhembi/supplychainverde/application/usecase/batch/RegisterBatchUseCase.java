package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.application.mapper.BatchDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterBatchUseCase {
    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final BatchDtoMapper mapper;

    @Transactional
    public BatchResponseDTO execute(BatchRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de lote é obrigatória.");
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + request.productId()));
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + request.supplierId()));

        Batch batch = Batch.builder()
                .product(product)
                .supplier(supplier)
                .quantity(request.quantity())
                .producedAt(request.producedAt())
                .build();
        return mapper.toResponse(batchRepository.save(batch), null, List.of());
    }
}
