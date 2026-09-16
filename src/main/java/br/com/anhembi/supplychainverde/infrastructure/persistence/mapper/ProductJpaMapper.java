package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ProductJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductJpaMapper {
    Product toDomain(ProductJpaEntity entity);
    ProductJpaEntity toJpaEntity(Product domain);
}
