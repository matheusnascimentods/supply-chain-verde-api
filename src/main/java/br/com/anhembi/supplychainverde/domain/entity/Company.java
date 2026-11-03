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
}
