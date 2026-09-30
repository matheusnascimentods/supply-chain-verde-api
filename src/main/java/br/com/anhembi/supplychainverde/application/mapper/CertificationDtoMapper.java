package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.certification.CertificationRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.certification.CertificationResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificationDtoMapper {
    Certification toDomain(CertificationRequestDTO request);

    @Mapping(source = "supplier.supplierId", target = "supplierId")
    CertificationResponseDTO toResponse(Certification certification);

    default CertificationResponseDTO toDto(Certification certification) {
        return toResponse(certification);
    }
}
