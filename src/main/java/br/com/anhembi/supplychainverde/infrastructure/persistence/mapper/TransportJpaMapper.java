package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Transport;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.TransportJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ChainJpaMapper.class)
public interface TransportJpaMapper {
    Transport toDomain(TransportJpaEntity entity);
    TransportJpaEntity toJpaEntity(Transport domain);
}
