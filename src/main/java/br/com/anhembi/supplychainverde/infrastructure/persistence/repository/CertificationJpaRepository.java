package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CertificationJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface CertificationJpaRepository extends JpaRepository<CertificationJpaEntity, Long> {
    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findBySupplierSupplierId(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findBySupplierSupplierIdAndStatus(Long supplierId, CertificationStatus status);

    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findByExpiresAtBetween(LocalDate startsAt, LocalDate endsAt);

    @Override
    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findAll();
}
