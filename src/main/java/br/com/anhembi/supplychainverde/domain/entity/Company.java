package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    private Long companyId;
    private String name;
    private Cnpj cnpj;
    private Address address;
    private String phone;
    private String email;
    private CompanyType type;
    private LocalDate registeredAt;
    // Menor usuário da companhia, só leitura; sai na Task 3 junto com o vínculo 1:1 fornecedor-usuário.
    private Long userId;
}
