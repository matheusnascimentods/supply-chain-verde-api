package br.com.anhembi.supplychainverde.application.usecase.product;

import br.com.anhembi.supplychainverde.application.dto.product.ProductRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {
    private final ProductRepository productRepository;

    public ProductResponseDTO execute(Long productId, ProductRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de produto é obrigatória.");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + productId));
        product.setName(request.name());
        product.setCategory(request.category());
        product.setUnit(request.unit());
        product.setDescription(request.description());
        Product updated = productRepository.save(product);
        return new ProductResponseDTO(updated.getProductId(), updated.getName(), updated.getCategory(), updated.getUnit(), updated.getDescription());
    }
}
