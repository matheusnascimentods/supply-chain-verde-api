package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.AuditLogJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = UserJpaMapper.class)
public interface AuditLogJpaMapper {
    AuditLog toDomain(AuditLogJpaEntity entity);
    AuditLogJpaEntity toJpaEntity(AuditLog domain);
}
