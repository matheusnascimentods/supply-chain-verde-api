package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.audit.AuditLogResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogDtoMapper {
    AuditLogResponseDTO toResponse(AuditLog auditLog);
    default AuditLogResponseDTO toDto(AuditLog auditLog) { return toResponse(auditLog); }
}
