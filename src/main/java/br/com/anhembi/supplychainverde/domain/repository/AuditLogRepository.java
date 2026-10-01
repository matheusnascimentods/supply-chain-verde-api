package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;

import java.time.LocalDate;
import java.util.List;

public interface AuditLogRepository {
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
