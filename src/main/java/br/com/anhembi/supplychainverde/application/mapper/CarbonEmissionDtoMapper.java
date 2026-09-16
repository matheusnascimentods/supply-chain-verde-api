package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.valueobject.EmissionFactor;
import java.math.BigDecimal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CarbonEmissionDtoMapper {
    CarbonEmission toDomain(CarbonEmissionRequestDTO request);
    CarbonEmissionResponseDTO toResponse(CarbonEmission emission);
    default CarbonEmissionResponseDTO toDto(CarbonEmission emission) { return toResponse(emission); }

    default BigDecimal map(EmissionFactor value) {
        return value == null ? null : value.value();
    }

    default EmissionFactor map(BigDecimal value) {
        return value == null ? null : new EmissionFactor(value);
    }
}
