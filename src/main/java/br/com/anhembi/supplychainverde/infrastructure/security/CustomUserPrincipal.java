package br.com.anhembi.supplychainverde.infrastructure.security;

import br.com.anhembi.supplychainverde.domain.enums.CompanyType;

/**
 * companyId e companyType são nulos só para o PLATFORM_ADMIN (o filtro do JWT garante). Os escopos derivados
 * são o filtro de cada listagem: nulo significa "sem filtro".
 */
public record CustomUserPrincipal(Long userId, String email, String role, Long companyId, CompanyType companyType) {
    public Long supplierId() {
        return companyType == CompanyType.SUPPLIER ? companyId : null;
    }

    public Long marketId() {
        return companyType == CompanyType.MARKET ? companyId : null;
    }

    public boolean isSupplierCompany() {
        return companyType == CompanyType.SUPPLIER;
    }
}
