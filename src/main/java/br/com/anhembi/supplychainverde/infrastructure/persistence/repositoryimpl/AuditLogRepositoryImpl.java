package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import br.com.anhembi.supplychainverde.domain.repository.AuditLogRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.AuditLogJpaEntity;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.AuditLogJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {
    private final EntityManager entityManager;
    private final AuditLogJpaMapper mapper;
    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findByFilters(
            LocalDate from,
            LocalDate to,
            AuditAction action,
            String userEmail,
            Long companyId,
            int limit,
            int offset
    ) {
        List<Long> emailUserIds = userJpaRepository.findIdsByEmailFilter(userEmail);
        if (emailUserIds != null && emailUserIds.isEmpty()) {
            return List.of();
        }
        StringBuilder jpql = new StringBuilder("""
                SELECT auditLog
                FROM AuditLogJpaEntity auditLog
                LEFT JOIN FETCH auditLog.user auditUser
                WHERE auditLog.performedAt >= :from
                  AND auditLog.performedAt < :toExclusive
                """);
        if (action != null) {
            jpql.append(" AND auditLog.action = :action");
        }
        if (emailUserIds != null) {
            jpql.append(" AND auditUser.userId IN :emailUserIds");
        }
        // RN-14: a companhia vê as ações dos próprios usuários; sem companhia (PLATFORM_ADMIN), tudo.
        if (companyId != null) {
            jpql.append(" AND auditUser.companyId = :companyId");
        }
        jpql.append(" ORDER BY auditLog.performedAt DESC, auditLog.logId DESC");

        TypedQuery<AuditLogJpaEntity> query = entityManager.createQuery(jpql.toString(), AuditLogJpaEntity.class)
                .setParameter("from", from.atStartOfDay())
                .setParameter("toExclusive", to.plusDays(1).atStartOfDay())
                .setFirstResult(offset)
                .setMaxResults(limit);
        if (action != null) {
            query.setParameter("action", action);
        }
        if (emailUserIds != null) {
            query.setParameter("emailUserIds", emailUserIds);
        }
        if (companyId != null) {
            query.setParameter("companyId", companyId);
        }

        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByFilters(LocalDate from, LocalDate to, AuditAction action, String userEmail, Long companyId) {
        List<Long> emailUserIds = userJpaRepository.findIdsByEmailFilter(userEmail);
        if (emailUserIds != null && emailUserIds.isEmpty()) {
            return 0;
        }
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(auditLog)
                FROM AuditLogJpaEntity auditLog
                LEFT JOIN auditLog.user auditUser
                WHERE auditLog.performedAt >= :from
                  AND auditLog.performedAt < :toExclusive
                """);
        if (action != null) {
            jpql.append(" AND auditLog.action = :action");
        }
        if (emailUserIds != null) {
            jpql.append(" AND auditUser.userId IN :emailUserIds");
        }
        // RN-14: a companhia vê as ações dos próprios usuários; sem companhia (PLATFORM_ADMIN), tudo.
        if (companyId != null) {
            jpql.append(" AND auditUser.companyId = :companyId");
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class)
                .setParameter("from", from.atStartOfDay())
                .setParameter("toExclusive", to.plusDays(1).atStartOfDay());
        if (action != null) {
            query.setParameter("action", action);
        }
        if (emailUserIds != null) {
            query.setParameter("emailUserIds", emailUserIds);
        }
        if (companyId != null) {
            query.setParameter("companyId", companyId);
        }
        return query.getSingleResult();
    }
}
