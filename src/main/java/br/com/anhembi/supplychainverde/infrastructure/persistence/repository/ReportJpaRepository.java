package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {
    List<ReportJpaEntity> findBySupplierSupplierId(Long supplierId);
}
