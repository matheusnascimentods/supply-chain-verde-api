package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.product.ProductRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductDtoMapper {
    @Mapping(target = "productId", ignore = true)
    Product toDomain(ProductRequestDTO request);

    @Mapping(target = "productId", ignore = true)
    void update(ProductRequestDTO request, @MappingTarget Product product);

    ProductResponseDTO toResponse(Product product);
}
