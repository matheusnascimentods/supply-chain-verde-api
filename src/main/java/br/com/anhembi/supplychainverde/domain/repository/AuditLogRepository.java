package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(Long logId);

    List<AuditLog> findAll();

    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByPerformedAtBetween(LocalDateTime startedAt, LocalDateTime endedAt);

    List<AuditLog> findByFilters(
            LocalDate from,
            LocalDate to,
            AuditAction action,
            String userEmail,
            int limit,
            int offset
    );

    long countByFilters(LocalDate from, LocalDate to, AuditAction action, String userEmail);
}
