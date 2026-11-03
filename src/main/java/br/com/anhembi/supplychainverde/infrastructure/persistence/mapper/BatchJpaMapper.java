package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.BatchJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductJpaMapper.class, SupplierJpaMapper.class})
public interface BatchJpaMapper {
    Batch toDomain(BatchJpaEntity entity);
    @Mapping(target = "marketId", ignore = true)
    @Mapping(target = "status", ignore = true)
    BatchJpaEntity toJpaEntity(Batch domain);
}
