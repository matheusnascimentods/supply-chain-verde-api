package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

public interface ReportRepository {
    Report save(Report report);
    Long generate(Long supplierId, Long marketId, LocalDate periodStartAt, LocalDate periodEndAt);

    Optional<Report> findById(Long reportId);

    List<Report> findAll();

    /** supplierId e marketId nulos não filtram. */
    List<Report> findPage(Long supplierId, Long marketId, int limit, int offset);

    long countPage(Long supplierId, Long marketId);

    /** marketId nulo conta os relatórios de todos os mercados. */
    Map<Long, Long> countBySupplierIds(Set<Long> supplierIds, Long marketId);

    List<Report> findBySupplierId(Long supplierId);

    List<Report> findBySupplierIdAndPeriodBetween(
            Long supplierId,
            LocalDate periodStartAt,
            LocalDate periodEndAt
    );

    void deleteById(Long reportId);
}
