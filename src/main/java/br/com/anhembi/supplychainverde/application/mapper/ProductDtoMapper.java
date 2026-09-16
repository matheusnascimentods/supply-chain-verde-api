package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.product.ProductRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductDtoMapper {
    Product toDomain(ProductRequestDTO request);
    ProductResponseDTO toResponse(Product product);
    default ProductResponseDTO toDto(Product product) { return toResponse(product); }
}
