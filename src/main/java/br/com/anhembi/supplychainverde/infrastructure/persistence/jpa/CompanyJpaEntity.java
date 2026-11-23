package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long companyId;

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

    @Column(name = "email", nullable = false)
    private String email;

    // RN-05: o tipo não muda depois do cadastro.
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private CompanyType type;

    // RN-01 (016): o banco põe o padrão (10), e nenhum endpoint altera o teto. @Generated relê o valor depois do INSERT.
    @Generated(event = EventType.INSERT)
    @Column(name = "max_users", nullable = false, insertable = false, updatable = false)
    private Integer maxUsers;

}
