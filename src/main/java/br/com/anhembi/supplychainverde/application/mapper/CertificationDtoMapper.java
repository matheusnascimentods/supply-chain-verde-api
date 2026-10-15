package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.certification.CertificationRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.certification.CertificationResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierCertificationDTO;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificationDtoMapper {
    @Mapping(target = "certificationId", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "status", ignore = true)
    Certification toDomain(CertificationRequestDTO request);

    @Mapping(target = "supplierId", source = "supplier.supplierId")
    CertificationResponseDTO toResponse(Certification certification);

    SupplierCertificationDTO toSupplierCertification(Certification certification);
}
