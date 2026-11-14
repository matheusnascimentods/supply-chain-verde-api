package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Invite;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.InviteJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InviteJpaMapper {
    Invite toDomain(InviteJpaEntity entity);
    InviteJpaEntity toJpaEntity(Invite domain);
}
