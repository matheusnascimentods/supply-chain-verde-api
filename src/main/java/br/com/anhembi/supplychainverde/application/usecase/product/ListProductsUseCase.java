package br.com.anhembi.supplychainverde.application.usecase.product;

import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.application.mapper.ProductDtoMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.FullTextSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ListProductsUseCase {
    private final ProductRepository productRepository;
    private final ProductDtoMapper mapper;

    public List<ProductResponseDTO> execute() {
        return productRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    public OffsetPageResponseDTO<ProductResponseDTO> execute(int limit, int offset, String search, Long supplierId) {
        String term = search == null ? "" : search.trim();
        String categoryCode = categoryCode(term);
        // RN-06: termo sem letras nem dígitos (ex.: "--") vale como busca vazia.
        boolean listAll = FullTextSearch.booleanQuery(term).isEmpty() && categoryCode.isEmpty();
        List<Product> products = listAll
                ? productRepository.findAll(supplierId, limit, offset)
                : productRepository.findBySearch(supplierId, term, categoryCode, limit, offset);
        long totalElements = listAll
                ? productRepository.countAll(supplierId)
                : productRepository.countBySearch(supplierId, term, categoryCode);
        List<ProductResponseDTO> items = products.stream()
                .map(mapper::toResponse)
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
}
