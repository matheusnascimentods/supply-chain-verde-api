package br.com.anhembi.supplychainverde.application.dto.plan;

import br.com.anhembi.supplychainverde.domain.entity.Plan;

public record PlanResponseDTO(String code, String name, int maxUsers) {
    public static PlanResponseDTO of(Plan plan) {
        return plan == null ? null : new PlanResponseDTO(plan.code(), plan.name(), plan.maxUsers());
    }
}
