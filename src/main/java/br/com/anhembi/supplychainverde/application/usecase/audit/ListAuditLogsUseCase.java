package br.com.anhembi.supplychainverde.application.usecase.audit;

import br.com.anhembi.supplychainverde.application.dto.audit.AuditLogResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAuditLogsUseCase {
    private final AuditLogRepository auditLogRepository;

    public List<AuditLogResponseDTO> execute() {
        return auditLogRepository.findAll().stream().map(this::toResponse).toList();
    }

    private AuditLogResponseDTO toResponse(AuditLog auditLog) {
        return new AuditLogResponseDTO(
                auditLog.getLogId(),
                auditLog.getUser() != null ? auditLog.getUser().getUserId() : null,
                auditLog.getAction(),
                auditLog.getAffectedTable(),
                auditLog.getPerformedAt()
        );
    }
}
