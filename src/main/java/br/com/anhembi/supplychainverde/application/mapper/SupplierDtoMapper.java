package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierDtoMapper {
    Supplier toDomain(SupplierRequestDTO request);
    SupplierResponseDTO toResponse(Supplier supplier);
    default SupplierResponseDTO toDto(Supplier supplier) { return toResponse(supplier); }

    default Cnpj map(String value) {
        return value == null ? null : new Cnpj(value);
    }

    default String map(Cnpj value) {
        return value == null ? null : value.value();
    }
}
