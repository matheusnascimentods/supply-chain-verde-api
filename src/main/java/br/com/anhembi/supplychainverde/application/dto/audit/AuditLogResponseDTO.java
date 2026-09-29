package br.com.anhembi.supplychainverde.application.dto.audit;
import java.time.LocalDateTime;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
public record AuditLogResponseDTO(Long logId, Long userId, String userEmail, AuditAction action, String affectedTable, LocalDateTime performedAt) {}
