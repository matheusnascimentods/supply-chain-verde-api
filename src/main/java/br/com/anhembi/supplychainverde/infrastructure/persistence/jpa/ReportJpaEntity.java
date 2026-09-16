package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierJpaEntity supplier;

    @Column(name = "period_start_at", nullable = false)
    private LocalDate periodStartAt;

    @Column(name = "period_end_at", nullable = false)
    private LocalDate periodEndAt;

    @Column(name = "total_co2_kg", nullable = false)
    private BigDecimal totalCo2Kg;

    @Column(name = "tracked_product_count", nullable = false)
    private Integer trackedProductCount;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
