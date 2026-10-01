package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ReportJpaEntity;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.ReportJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.ReportJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {
    private final ReportJpaRepository jpaRepository;
    private final ReportJpaMapper mapper;
    private final EntityManager entityManager;

    @Override
    public Report save(Report report) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(report)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Report> findById(Long reportId) {
        return jpaRepository.findById(reportId).map(mapper::toDomain);
    }

    @Override
    public List<Report> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> findPage(Long supplierId, int limit, int offset) {
        StringBuilder jpql = new StringBuilder("""
                SELECT report
                FROM ReportJpaEntity report
                JOIN FETCH report.supplier supplier
                JOIN FETCH supplier.address
                """);
        if (supplierId != null) {
            jpql.append(" WHERE supplier.supplierId = :supplierId");
        }
        jpql.append(" ORDER BY report.generatedAt DESC, report.reportId DESC");

        TypedQuery<ReportJpaEntity> query = entityManager.createQuery(jpql.toString(), ReportJpaEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit);
        if (supplierId != null) {
            query.setParameter("supplierId", supplierId);
        }
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(Long supplierId) {
        String jpql = supplierId == null
                ? "SELECT COUNT(report) FROM ReportJpaEntity report"
                : "SELECT COUNT(report) FROM ReportJpaEntity report WHERE report.supplier.supplierId = :supplierId";
        var query = entityManager.createQuery(jpql, Long.class);
        if (supplierId != null) {
            query.setParameter("supplierId", supplierId);
        }
        return query.getSingleResult();
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
