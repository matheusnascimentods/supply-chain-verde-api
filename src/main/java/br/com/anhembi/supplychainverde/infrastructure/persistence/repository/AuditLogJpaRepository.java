package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, Long> {
    List<AuditLogJpaEntity> findByUserUserId(Long userId);
    List<AuditLogJpaEntity> findByPerformedAtBetween(LocalDateTime startedAt, LocalDateTime endedAt);
}
