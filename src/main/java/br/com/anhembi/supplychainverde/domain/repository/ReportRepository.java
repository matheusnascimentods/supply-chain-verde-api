package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

public interface ReportRepository {
    Report save(Report report);

    Optional<Report> findById(Long reportId);

    List<Report> findAll();

    List<Report> findPage(Long supplierId, int limit, int offset);

    long countPage(Long supplierId);

    Map<Long, Long> countBySupplierIds(Set<Long> supplierIds);

    List<Report> findBySupplierId(Long supplierId);

    List<Report> findBySupplierIdAndPeriodBetween(
            Long supplierId,
            LocalDate periodStartAt,
            LocalDate periodEndAt
    );

    void deleteById(Long reportId);
}
