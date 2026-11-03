package br.com.anhembi.supplychainverde.application.dto.company;

import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;

public record RegisterCompanyRequestDTO(String planCode, CompanyType type, CompanyData company, AdminData admin) {
    public record CompanyData(String name, String cnpj, String email, String phone, AddressRequestDTO address) {}

    public record AdminData(String name, String email, String password) {}
}
