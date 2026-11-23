package br.com.anhembi.supplychainverde.application.dto.company;

import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;

/** RF-04 (016): planCode saiu; se vier, o Jackson ignora. */
public record RegisterCompanyRequestDTO(CompanyType type, CompanyData company, AdminData admin) {
    public record CompanyData(String name, String cnpj, String email, String phone, AddressRequestDTO address) {}

    public record AdminData(String name, String email, String password) {}
}
