package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {
    List<ReportJpaEntity> findBySupplierSupplierId(Long supplierId);

    @Query("select report.supplier.supplierId, count(report) from ReportJpaEntity report " +
            "where report.supplier.supplierId in :supplierIds group by report.supplier.supplierId")
    List<Object[]> countBySupplierIds(@Param("supplierIds") List<Long> supplierIds);
}
