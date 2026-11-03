package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository {
    List<Plan> findAll();

    Optional<Plan> findByCode(String code);

    Optional<Plan> findActiveByCompanyId(Long companyId);
}
