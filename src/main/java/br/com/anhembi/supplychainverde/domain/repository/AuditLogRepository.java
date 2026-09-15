package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(Long logId);

    List<AuditLog> findAll();

    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByPerformedAtBetween(LocalDateTime startedAt, LocalDateTime endedAt);
}
