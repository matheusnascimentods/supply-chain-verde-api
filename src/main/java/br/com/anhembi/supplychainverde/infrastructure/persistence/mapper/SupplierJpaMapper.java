package br.com.anhembi.supplychainverde.infrastructure.persistence.mapper;

import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.SupplierJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = AddressJpaMapper.class)
public interface SupplierJpaMapper {
    Supplier toDomain(SupplierJpaEntity entity);
    SupplierJpaEntity toJpaEntity(Supplier domain);

    default Cnpj map(String cnpj) {
        return cnpj == null ? null : new Cnpj(cnpj);
    }

    default String map(Cnpj cnpj) {
        return cnpj == null ? null : cnpj.value();
    }

    default LocalDate map(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    // Só preenche o objeto: o banco ignora (updatable = false) e o @CreationTimestamp sobrescreve no insert.
    default LocalDateTime map(LocalDate value) {
        return value == null ? null : value.atStartOfDay();
    }
}
