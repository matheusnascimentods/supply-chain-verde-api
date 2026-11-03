package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "company")
@SQLRestriction("type = 'SUPPLIER'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long supplierId;

    @Column(name = "name", nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cnpj", nullable = false, unique = true, length = 14)
    private String cnpj;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private AddressJpaEntity address;

    @Column(name = "phone")
    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    // ponytail: tipo fixo e email placeholder (padrão da V8, com o CNPJ porque o id ainda não existe)
    // até a Task 2 trazer o cadastro de companhia.
    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Column(name = "type", nullable = false, updatable = false)
    private String type;

    // supplier.user_id saiu na V8: o vínculo agora é users.company_id. Só leitura até a Task 3.
    @Formula("(SELECT MIN(u.id) FROM users u WHERE u.company_id = id)")
    private Long userId;

    @PrePersist
    void fillCompanyDefaults() {
        type = "SUPPLIER";
        if (email == null) email = "fornecedor" + cnpj + "@supplychainverde.local";
    }
}
