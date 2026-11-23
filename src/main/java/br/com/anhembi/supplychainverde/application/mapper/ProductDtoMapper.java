package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.product.ProductRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository.CatalogItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductDtoMapper {
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "supplierId", ignore = true)
    Product toDomain(ProductRequestDTO request);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "supplierId", ignore = true)
    void update(ProductRequestDTO request, @MappingTarget Product product);

    @Mapping(target = "supplierName", ignore = true)
    @Mapping(target = "co2KgPerUnit", ignore = true)
    @Mapping(target = "acceptedBatchesLast30Days", ignore = true)
    ProductResponseDTO toResponse(Product product);

    ProductResponseDTO toResponse(CatalogItem item);
}
