package br.com.anhembi.supplychainverde.application.dto.audit;
import java.time.LocalDateTime;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import java.util.Map;
public record AuditLogResponseDTO(Long logId, Long userId, String userEmail, AuditAction action, String affectedTable, Long affectedEntityId, Map<String, Object> beforeData, Map<String, Object> afterData, LocalDateTime performedAt) {}
