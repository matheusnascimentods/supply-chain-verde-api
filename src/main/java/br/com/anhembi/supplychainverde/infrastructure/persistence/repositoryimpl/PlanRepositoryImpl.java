package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Plan;
import br.com.anhembi.supplychainverde.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Catálogo fixo e só de leitura (a V7 cria as linhas): JdbcTemplate basta, sem entidade JPA.
@Repository
@RequiredArgsConstructor
public class PlanRepositoryImpl implements PlanRepository {
    private static final String SELECT = "SELECT p.id, p.code, p.name, p.max_users FROM plan p ";
    private static final RowMapper<Plan> ROW = (rs, rowNum) ->
            new Plan(rs.getLong("id"), rs.getString("code"), rs.getString("name"), rs.getInt("max_users"));

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Plan> findAll() {
        return jdbcTemplate.query(SELECT + "ORDER BY p.max_users", ROW);
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return jdbcTemplate.query(SELECT + "WHERE p.code = ?", ROW, code).stream().findFirst();
    }

    @Override
    public Optional<Plan> findActiveByCompanyId(Long companyId) {
        return jdbcTemplate.query(SELECT + "JOIN company_plan cp ON cp.plan_id = p.id WHERE cp.active_company_id = ?",
                ROW, companyId).stream().findFirst();
    }
}
