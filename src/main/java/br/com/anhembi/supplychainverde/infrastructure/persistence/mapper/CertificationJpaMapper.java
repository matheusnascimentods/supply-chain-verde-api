package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CertificationJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = SupplierJpaMapper.class)
public interface CertificationJpaMapper {
    Certification toDomain(CertificationJpaEntity entity);
    CertificationJpaEntity toJpaEntity(Certification domain);
}
