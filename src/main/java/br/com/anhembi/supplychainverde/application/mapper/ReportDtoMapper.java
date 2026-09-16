package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.report.ReportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.report.ReportResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Report;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReportDtoMapper {
    Report toDomain(ReportRequestDTO request);
    ReportResponseDTO toResponse(Report report);
    default ReportResponseDTO toDto(Report report) { return toResponse(report); }
}
