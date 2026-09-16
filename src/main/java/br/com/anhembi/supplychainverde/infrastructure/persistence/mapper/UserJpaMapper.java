package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.UserJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserJpaMapper {
    User toDomain(UserJpaEntity entity);
    UserJpaEntity toJpaEntity(User domain);
}
