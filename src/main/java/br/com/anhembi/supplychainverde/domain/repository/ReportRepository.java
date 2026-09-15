package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {
    Report save(Report report);

    Optional<Report> findById(Long reportId);

    List<Report> findAll();

    List<Report> findBySupplierId(Long supplierId);

    List<Report> findBySupplierIdAndPeriodBetween(
            Long supplierId,
            LocalDate periodStartAt,
            LocalDate periodEndAt
    );

    void deleteById(Long reportId);
}
