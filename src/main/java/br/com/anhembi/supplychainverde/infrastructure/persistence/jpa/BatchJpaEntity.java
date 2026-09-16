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
    @Column(name = "batch_id")
    private Long batchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierJpaEntity supplier;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "produced_at", nullable = false)
    private LocalDate producedAt;
}
