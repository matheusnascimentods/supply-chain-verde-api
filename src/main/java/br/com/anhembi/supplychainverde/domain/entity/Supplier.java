package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {
    private Long supplierId;
    private String name;
    private Cnpj cnpj;
    private Address address;
    private String phone;
    private LocalDate registeredAt;
}
