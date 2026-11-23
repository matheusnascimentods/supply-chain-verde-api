package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.CompanyJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.FullTextSearch;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.CompanyJpaRepository;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {
    private final CompanyJpaRepository jpaRepository;
    private final CompanyJpaMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Company save(Company company) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(company)));
    }

    @Override
    public Optional<Company> findById(Long companyId) {
        return jpaRepository.findById(companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<Company> findSupplierById(Long supplierId) {
        return jpaRepository.findByCompanyIdAndType(supplierId, CompanyType.SUPPLIER).map(mapper::toDomain);
    }

    @Override
    public List<Company> findAllSuppliers() {
        return jpaRepository.findAllByType(CompanyType.SUPPLIER).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void subscribe(Long companyId, Long planId) {
        jdbcTemplate.update("INSERT INTO company_plan (company_id, plan_id) VALUES (?, ?)", companyId, planId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> findBySearch(String search) {
        return jpaRepository.findBySearch(FullTextSearch.booleanQuery(search), FullTextSearch.cnpjQuery(search)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCnpj(Cnpj cnpj) {
        return jpaRepository.existsByCnpj(cnpj.value());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> findAllById(Collection<Long> supplierIds) {
        return jpaRepository.findAllById(supplierIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countBySearch(String search, boolean workedWithOnly, Long marketId) {
        return jpaRepository.countBySearch(FullTextSearch.booleanQuery(search), FullTextSearch.cnpjQuery(search),
                workedWithOnly, marketId);
    }

    @Override
    public List<Long> findIdsRankedByScore(String search, boolean workedWithOnly, Long marketId, int limit, int offset) {
        return jpaRepository.findIdsRankedByScore(FullTextSearch.booleanQuery(search), FullTextSearch.cnpjQuery(search),
                workedWithOnly, marketId, limit, offset);
    }

    @Override
    public Set<Long> findWorkedWithIds(Long marketId, Collection<Long> supplierIds) {
        return marketId == null || supplierIds.isEmpty() ? Set.of()
                : Set.copyOf(jpaRepository.findWorkedWithIds(marketId, supplierIds));
    }

    @Override
    public void lockById(Long companyId) {
        jdbcTemplate.queryForObject("SELECT id FROM company WHERE id = ? FOR UPDATE", Long.class, companyId);
    }

    @Override
    public List<SupplierScore> findScoresByIds(Collection<Long> supplierIds) {
        return supplierIds.isEmpty() ? List.of() : jpaRepository.findScoresByIds(supplierIds);
    }

}
