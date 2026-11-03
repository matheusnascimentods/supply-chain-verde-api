package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import jakarta.persistence.*;
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

    @Column(name = "produced_at", nullable = false)
    private LocalDate producedAt;

    // ponytail: o lote nasce ACTIVE no mercado padrão até a Task 4 trazer o pedido (PENDING) do mercado logado.
    @Column(name = "market_id", nullable = false, updatable = false)
    private Long marketId;

    @Column(name = "status", nullable = false, updatable = false)
    private String status;

    @PrePersist
    void fillStatus() {
        if (status == null) status = "ACTIVE";
    }
}
