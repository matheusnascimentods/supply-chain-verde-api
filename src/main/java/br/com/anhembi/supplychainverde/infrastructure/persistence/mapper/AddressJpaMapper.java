package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.AddressJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressJpaMapper {
    Address toDomain(AddressJpaEntity entity);
    AddressJpaEntity toJpaEntity(Address domain);
}
