package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CertificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CertificationJpaRepository extends JpaRepository<CertificationJpaEntity, Long> {
    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findBySupplierCompanyId(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findBySupplierCompanyIdIn(Collection<Long> supplierIds);

    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findBySupplierCompanyIdAndStatus(Long supplierId, CertificationStatus status);

    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findBySupplierCompanyIdAndIssuedAtLessThanEqualAndExpiresAtGreaterThanEqualOrderByIssuedAt(
            Long supplierId, LocalDate end, LocalDate start);

    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    Page<CertificationJpaEntity> findByStatus(CertificationStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    Page<CertificationJpaEntity> findAll(Pageable pageable);

    @Query("""
            select count(c) from CertificationJpaEntity c
            where c.expiresAt between :startsAt and :endsAt
              and (:supplierId is null or c.supplier.companyId = :supplierId)
            """)
    long countByExpiresAtBetween(@Param("startsAt") LocalDate startsAt, @Param("endsAt") LocalDate endsAt,
            @Param("supplierId") Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    @Query("""
            select c from CertificationJpaEntity c
            where (:status is null or c.status = :status)
              and (:supplierId is null or c.supplier.companyId = :supplierId)
            """)
    Page<CertificationJpaEntity> findPage(@Param("status") CertificationStatus status, @Param("supplierId") Long supplierId,
            Pageable pageable);

    long countByStatus(CertificationStatus status);

    @Override
    @EntityGraph(attributePaths = {"supplier", "supplier.address"})
    List<CertificationJpaEntity> findAll();
}
