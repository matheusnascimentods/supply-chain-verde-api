package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.BatchJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ProductJpaMapper.class, SupplierJpaMapper.class})
public interface BatchJpaMapper {
    Batch toDomain(BatchJpaEntity entity);
    BatchJpaEntity toJpaEntity(Batch domain);
}
