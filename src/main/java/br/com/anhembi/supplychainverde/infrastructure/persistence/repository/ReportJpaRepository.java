package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {
    List<ReportJpaEntity> findBySupplierCompanyId(Long supplierId);

    @Query("select report.supplier.companyId, count(report) from ReportJpaEntity report " +
            "where report.supplier.companyId in :supplierIds and (:marketId is null or report.marketId = :marketId) " +
            "group by report.supplier.companyId")
    List<Object[]> countBySupplierIds(@Param("supplierIds") List<Long> supplierIds, @Param("marketId") Long marketId);
}
