package br.com.anhembi.supplychainverde.application.usecase.product;

import br.com.anhembi.supplychainverde.application.dto.product.ProductRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.application.mapper.ProductDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterProductUseCase {
    private final ProductRepository productRepository;
    private final ProductDtoMapper mapper;

    @Transactional
    /** RN-16: o produto nasce no fornecedor de quem cadastra; supplierId nulo (não é fornecedor) dá 403. */
    public ProductResponseDTO execute(ProductRequestDTO request, Long supplierId) {
        if (supplierId == null) throw new AccessDeniedException("Só o fornecedor cadastra produtos.");
        if (request == null) throw new ValidationException("Requisição de produto é obrigatória.");
        Product product = mapper.toDomain(request);
        product.setSupplierId(supplierId);
        return mapper.toResponse(productRepository.save(product));
    }
}
