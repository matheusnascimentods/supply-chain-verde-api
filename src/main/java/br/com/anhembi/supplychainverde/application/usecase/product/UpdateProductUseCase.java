package br.com.anhembi.supplychainverde.application.usecase.product;

import br.com.anhembi.supplychainverde.application.dto.product.ProductRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.application.mapper.ProductDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {
    private final ProductRepository productRepository;
    private final ProductDtoMapper mapper;

    @Transactional
    public ProductResponseDTO execute(Long productId, ProductRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de produto é obrigatória.");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + productId));
        mapper.update(request, product);
        return mapper.toResponse(productRepository.save(product));
    }
}
