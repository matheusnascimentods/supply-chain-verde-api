package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import br.com.anhembi.supplychainverde.domain.repository.AuditLogRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.AuditLogJpaEntity;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.AuditLogJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.AuditLogJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {
    private final AuditLogJpaRepository jpaRepository;
    private final AuditLogJpaMapper mapper;
    private final EntityManager entityManager;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(auditLog)));
    }

    @Override
    public Optional<AuditLog> findById(Long logId) {
        return jpaRepository.findById(logId).map(mapper::toDomain);
    }

    @Override
    public List<AuditLog> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        return jpaRepository.findByUserUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByPerformedAtBetween(LocalDateTime startedAt, LocalDateTime endedAt) {
        return jpaRepository.findByPerformedAtBetween(startedAt, endedAt).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findByFilters(
            LocalDate from,
            LocalDate to,
            AuditAction action,
            String userEmail,
            int limit,
            int offset
    ) {
        StringBuilder jpql = new StringBuilder("""
                SELECT auditLog
                FROM AuditLogJpaEntity auditLog
                JOIN FETCH auditLog.user auditUser
                WHERE auditLog.performedAt >= :from
                  AND auditLog.performedAt < :toExclusive
                """);
        if (action != null) {
            jpql.append(" AND auditLog.action = :action");
        }
        if (userEmail != null && !userEmail.isBlank()) {
            jpql.append(" AND LOWER(auditUser.email) LIKE :userEmailPattern");
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
        if (userEmail != null && !userEmail.isBlank()) {
            query.setParameter("userEmailPattern", "%" + userEmail.toLowerCase(Locale.ROOT) + "%");
        }

        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByFilters(LocalDate from, LocalDate to, AuditAction action, String userEmail) {
        StringBuilder jpql = new StringBuilder("""
                SELECT COUNT(auditLog)
                FROM AuditLogJpaEntity auditLog
                JOIN auditLog.user auditUser
                WHERE auditLog.performedAt >= :from
                  AND auditLog.performedAt < :toExclusive
                """);
        if (action != null) {
            jpql.append(" AND auditLog.action = :action");
        }
        if (userEmail != null && !userEmail.isBlank()) {
            jpql.append(" AND LOWER(auditUser.email) LIKE :userEmailPattern");
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class)
                .setParameter("from", from.atStartOfDay())
                .setParameter("toExclusive", to.plusDays(1).atStartOfDay());
        if (action != null) {
            query.setParameter("action", action);
        }
        if (userEmail != null && !userEmail.isBlank()) {
            query.setParameter("userEmailPattern", "%" + userEmail.toLowerCase(Locale.ROOT) + "%");
        }
        return query.getSingleResult();
    }
}
