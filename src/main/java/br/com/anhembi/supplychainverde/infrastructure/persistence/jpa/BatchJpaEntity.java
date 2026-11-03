package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long batchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private CompanyJpaEntity supplier;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    // RN-20: nulo enquanto o lote está PENDING; a etapa PRODUCTION preenche.
    @Column(name = "produced_at")
    private LocalDate producedAt;

    @Column(name = "market_id", nullable = false, updatable = false)
    private Long marketId;

    // Só leitura: evita um ManyToOne preguiçoso que estouraria fora da sessão (open-in-view desligado).
    @Formula("(SELECT c.name FROM company c WHERE c.id = market_id)")
    private String marketName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}
