package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierCertificationDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = AddressDtoMapper.class)
public interface SupplierDtoMapper {
    @Mapping(target = "supplierId", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Supplier toDomain(SupplierRequestDTO request);

    SupplierResponseDTO toResponse(Supplier supplier);

    SupplierRankingDTO toRanking(Supplier supplier, BigDecimal sustainabilityScore, Integer activeCertificationCount,
            BigDecimal totalCo2Kg, long reportCount, List<SupplierCertificationDTO> certifications, BigDecimal co2KgPerUnit);

    default Cnpj map(String value) {
        return value == null ? null : new Cnpj(value);
    }

    default String map(Cnpj value) {
        return value == null ? null : value.value();
    }
}
