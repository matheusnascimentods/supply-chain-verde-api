package br.com.anhembi.supplychainverde.application.usecase.product;

import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListProductsUseCase {
    private final ProductRepository productRepository;

    public List<ProductResponseDTO> execute() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ProductResponseDTO toResponse(Product product) {
        return new ProductResponseDTO(product.getProductId(), product.getName(), product.getCategory(), product.getUnit(), product.getDescription());
    }
}
