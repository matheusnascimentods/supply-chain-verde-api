package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.BatchDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegisterBatchUseCase {
    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository supplierRepository;
    private final BatchDtoMapper mapper;

    /** RN-17 e RN-18: o mercado pede o lote a um fornecedor, com um produto desse fornecedor; o lote nasce PENDING. */
    @Transactional
    public BatchResponseDTO execute(BatchRequestDTO request, Long marketId) {
        if (marketId == null) throw new AccessDeniedException("Só um mercado cria lotes.");
        if (request == null || request.productId() == null || request.supplierId() == null) {
            throw new ValidationException("Produto e fornecedor do lote são obrigatórios.");
        }
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + request.productId()));
        Company supplier = supplierRepository.findSupplierById(request.supplierId())
                .orElseThrow(() -> new CompanyNotFoundException(request.supplierId()));
        // Antes da FK (product_id, supplier_id) estourar como 500.
        if (!supplier.getCompanyId().equals(product.getSupplierId())) {
            throw new ValidationException("O produto " + product.getProductId() + " não é do fornecedor " + supplier.getCompanyId() + ".");
        }

        // O nome vem daqui: a @Formula de marketName só é lida quando o lote é carregado de novo.
        Company market = supplierRepository.findById(marketId).orElseThrow(() -> new CompanyNotFoundException(marketId));

        Batch batch = Batch.builder()
                .product(product)
                .supplier(supplier)
                .marketId(marketId)
                .marketName(market.getName())
                .status(BatchStatus.PENDING)
                .quantity(request.quantity())
                .build();
        return mapper.toResponse(batchRepository.save(batch), null, List.of());
    }
}
