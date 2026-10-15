package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.valueobject.EmissionFactor;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CarbonEmissionDtoMapper {
    @Mapping(target = "chainId", source = "chain.chainId")
    CarbonEmissionResponseDTO toResponse(CarbonEmission emission);

    default BigDecimal map(EmissionFactor value) {
        return value == null ? null : value.value();
    }
}
