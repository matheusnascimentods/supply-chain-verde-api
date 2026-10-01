package br.com.anhembi.supplychainverde.application.usecase.product;

import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ListProductsUseCase {
    private final ProductRepository productRepository;

    public List<ProductResponseDTO> execute() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    public OffsetPageResponseDTO<ProductResponseDTO> execute(int limit, int offset, String search) {
        String term = search == null ? "" : search.trim();
        String categoryCode = categoryCode(term);
        List<Product> products = term.isEmpty()
                ? productRepository.findAll(limit, offset)
                : productRepository.findBySearch(term, categoryCode, limit, offset);
        long totalElements = term.isEmpty()
                ? productRepository.countAll()
                : productRepository.countBySearch(term, categoryCode);
        List<ProductResponseDTO> items = products.stream()
                .map(this::toResponse)
                .toList();

        return OffsetPageResponseDTO.of(items, limit, offset, totalElements);
    }

    private String categoryCode(String search) {
        String normalizedSearch = normalize(search);
        for (ProductCategory category : ProductCategory.values()) {
            if (normalize(category.name()).equals(normalizedSearch)
                    || normalize(categoryLabel(category)).equals(normalizedSearch)) {
                return category.name();
            }
        }
        return "";
    }

    private String categoryLabel(ProductCategory category) {
        return switch (category) {
            case AGRICULTURE -> "Agricultura";
            case LIVESTOCK -> "Pecuária";
            case PROCESSED_FOOD -> "Alimentos processados";
            case TEXTILE -> "Têxtil";
            case FORESTRY -> "Florestal";
            case OTHER -> "Outro";
        };
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private ProductResponseDTO toResponse(Product product) {
        return new ProductResponseDTO(product.getProductId(), product.getName(), product.getCategory(), product.getUnit(), product.getDescription());
    }
}
