package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ChainJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {BatchJpaMapper.class, AddressJpaMapper.class, UserJpaMapper.class})
public interface ChainJpaMapper {
    Chain toDomain(ChainJpaEntity entity);
    ChainJpaEntity toJpaEntity(Chain domain);
}
