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
public class RegisterProductUseCase {
    private final ProductRepository productRepository;

    public ProductResponseDTO execute(ProductRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de produto é obrigatória.");

        Product product = Product.builder()
                .name(request.name())
                .category(request.category())
                .unit(request.unit())
                .description(request.description())
                .build();
        Product saved = productRepository.save(product);
        return new ProductResponseDTO(saved.getProductId(), saved.getName(), saved.getCategory(), saved.getUnit(), saved.getDescription());
    }
}
