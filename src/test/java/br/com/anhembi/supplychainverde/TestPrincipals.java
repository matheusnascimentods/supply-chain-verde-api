package br.com.anhembi.supplychainverde;

import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;

/** Principals de teste. Sem companhia, as listagens não filtram (visão do PLATFORM_ADMIN). */
public final class TestPrincipals {
    private TestPrincipals() {
    }

    /** supplierId preenchido vira companhia fornecedora; nulo, sem companhia. */
    public static CustomUserPrincipal of(Long userId, String email, String role, Long supplierId) {
        return new CustomUserPrincipal(userId, email, role, supplierId, supplierId == null ? null : CompanyType.SUPPLIER);
    }

    public static CustomUserPrincipal market(Long userId, String role, Long marketId) {
        return new CustomUserPrincipal(userId, "market@example.com", role, marketId, CompanyType.MARKET);
    }
}
