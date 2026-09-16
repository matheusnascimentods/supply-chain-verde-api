package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ReportJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = SupplierJpaMapper.class)
public interface ReportJpaMapper {
    Report toDomain(ReportJpaEntity entity);
    ReportJpaEntity toJpaEntity(Report domain);
}
