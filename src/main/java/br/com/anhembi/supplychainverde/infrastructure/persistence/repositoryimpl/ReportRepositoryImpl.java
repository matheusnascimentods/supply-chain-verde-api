package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.ReportJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.ReportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {
    private final ReportJpaRepository jpaRepository;
    private final ReportJpaMapper mapper;

    @Override
    public Report save(Report report) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(report)));
    }

    @Override
    public Optional<Report> findById(Long reportId) {
        return jpaRepository.findById(reportId).map(mapper::toDomain);
    }

    @Override
    public List<Report> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Report> findBySupplierId(Long supplierId) {
        return jpaRepository.findBySupplierSupplierId(supplierId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Report> findBySupplierIdAndPeriodBetween(Long supplierId, LocalDate periodStartAt, LocalDate periodEndAt) {
        return jpaRepository.findBySupplierSupplierId(supplierId).stream()
                .map(mapper::toDomain)
                .filter(report -> !report.getPeriodStartAt().isBefore(periodStartAt) && !report.getPeriodEndAt().isAfter(periodEndAt))
                .toList();
    }

    @Override
    public void deleteById(Long reportId) {
        jpaRepository.deleteById(reportId);
    }
}
