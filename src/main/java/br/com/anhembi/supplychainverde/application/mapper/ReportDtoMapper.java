package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.report.ReportResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportDtoMapper {
    @Mapping(target = "supplierId", source = "supplier.supplierId")
    ReportResponseDTO toResponse(Report report);
}
