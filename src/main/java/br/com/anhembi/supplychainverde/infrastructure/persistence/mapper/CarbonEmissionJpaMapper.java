package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.valueobject.EmissionFactor;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CarbonEmissionJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ChainJpaMapper.class)
public interface CarbonEmissionJpaMapper {
    CarbonEmission toDomain(CarbonEmissionJpaEntity entity);
    CarbonEmissionJpaEntity toJpaEntity(CarbonEmission domain);

    default EmissionFactor map(BigDecimal value) {
        return value == null ? null : new EmissionFactor(value);
    }

    default BigDecimal map(EmissionFactor value) {
        return value == null ? null : value.value();
    }

    default LocalDate map(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    // Só preenche o objeto: o banco ignora (updatable = false) e o @CreationTimestamp sobrescreve no insert.
    default LocalDateTime map(LocalDate value) {
        return value == null ? null : value.atStartOfDay();
    }
}
