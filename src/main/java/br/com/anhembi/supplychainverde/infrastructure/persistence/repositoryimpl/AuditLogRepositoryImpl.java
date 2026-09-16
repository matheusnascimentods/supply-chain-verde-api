package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.repository.AuditLogRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.AuditLogJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {
    private final AuditLogJpaRepository jpaRepository;
    private final AuditLogJpaMapper mapper;

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
}
