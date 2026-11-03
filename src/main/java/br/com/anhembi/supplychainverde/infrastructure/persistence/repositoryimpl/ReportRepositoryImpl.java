package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Report;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.ReportJpaEntity;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.ReportJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.ReportJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepository {
    private final ReportJpaRepository jpaRepository;
    private final ReportJpaMapper mapper;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Report save(Report report) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(report)));
    }

    @Override
    public Long generate(Long supplierId, Long marketId, LocalDate periodStartAt, LocalDate periodEndAt) {
        // Mesma conexão nas duas chamadas: o aspect exige transação ativa para generate.
        jdbcTemplate.update("CALL sp_generate_sustainability_report(?, ?, ?, ?, @report_id)",
                supplierId, marketId, periodStartAt, periodEndAt);
        return jdbcTemplate.queryForObject("SELECT @report_id", Long.class);
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
    public List<Report> findPage(Long supplierId, Long marketId, int limit, int offset) {
        StringBuilder jpql = new StringBuilder("""
                SELECT report
                FROM ReportJpaEntity report
                JOIN FETCH report.supplier supplier
                JOIN FETCH supplier.address
                """);
        jpql.append(" WHERE (:supplierId IS NULL OR supplier.companyId = :supplierId)")
                .append(" AND (:marketId IS NULL OR report.marketId = :marketId)");
        jpql.append(" ORDER BY report.generatedAt DESC, report.reportId DESC");

        TypedQuery<ReportJpaEntity> query = entityManager.createQuery(jpql.toString(), ReportJpaEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .setParameter("supplierId", supplierId)
                .setParameter("marketId", marketId);
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(Long supplierId, Long marketId) {
        return entityManager.createQuery("""
                        SELECT COUNT(report) FROM ReportJpaEntity report
                        WHERE (:supplierId IS NULL OR report.supplier.companyId = :supplierId)
                          AND (:marketId IS NULL OR report.marketId = :marketId)
                        """, Long.class)
                .setParameter("supplierId", supplierId)
                .setParameter("marketId", marketId)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countBySupplierIds(Set<Long> supplierIds, Long marketId) {
        if (supplierIds.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.countBySupplierIds(List.copyOf(supplierIds), marketId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    @Override
    public List<Report> findBySupplierId(Long supplierId) {
        return jpaRepository.findBySupplierCompanyId(supplierId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Report> findBySupplierIdAndPeriodBetween(Long supplierId, LocalDate periodStartAt, LocalDate periodEndAt) {
        return jpaRepository.findBySupplierCompanyId(supplierId).stream()
                .map(mapper::toDomain)
                .filter(report -> !report.getPeriodStartAt().isBefore(periodStartAt) && !report.getPeriodEndAt().isAfter(periodEndAt))
                .toList();
    }

    @Override
    public void deleteById(Long reportId) {
        jpaRepository.deleteById(reportId);
    }
}
