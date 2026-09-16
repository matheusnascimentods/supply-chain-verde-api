package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "certification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certification_id")
    private Long certificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierJpaEntity supplier;

    @Column(name = "certification", nullable = false)
    private String certification;

    @Column(name = "issuing_body", nullable = false)
    private String issuingBody;

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CertificationStatus status;
}
