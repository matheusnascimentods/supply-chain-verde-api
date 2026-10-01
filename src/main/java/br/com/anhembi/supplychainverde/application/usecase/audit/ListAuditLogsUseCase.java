package br.com.anhembi.supplychainverde.application.usecase.audit;

import br.com.anhembi.supplychainverde.application.dto.audit.AuditLogResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import br.com.anhembi.supplychainverde.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ListAuditLogsUseCase {
    private final AuditLogRepository auditLogRepository;

    public List<AuditLogResponseDTO> execute() {
        return auditLogRepository.findAll().stream().map(this::toResponse).toList();
    }

    public OffsetPageResponseDTO<AuditLogResponseDTO> execute(
            LocalDate from,
            LocalDate to,
            AuditAction action,
            String userEmail,
            int limit,
            int offset
    ) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual à data final.");
        }
        String emailFilter = userEmail == null || userEmail.isBlank()
                ? null
                : userEmail.trim().toLowerCase(Locale.ROOT);
        List<AuditLog> logs = auditLogRepository.findByFilters(from, to, action, emailFilter, limit, offset);
        long totalElements = auditLogRepository.countByFilters(from, to, action, emailFilter);
        List<AuditLogResponseDTO> items = logs.stream().map(this::toResponse).toList();
        return OffsetPageResponseDTO.of(items, limit, offset, totalElements);
    }

    private AuditLogResponseDTO toResponse(AuditLog auditLog) {
        return new AuditLogResponseDTO(
                auditLog.getLogId(),
                auditLog.getUser() != null ? auditLog.getUser().getUserId() : null,
                auditLog.getUser() != null ? auditLog.getUser().getEmail() : null,
                auditLog.getAction(),
                auditLog.getAffectedTable(),
                auditLog.getPerformedAt()
        );
    }
}
