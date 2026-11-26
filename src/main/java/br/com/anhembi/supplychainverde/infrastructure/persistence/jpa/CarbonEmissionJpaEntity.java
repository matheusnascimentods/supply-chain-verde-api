package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.EnergySource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carbon_emission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarbonEmissionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long emissionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chain_id", nullable = false, unique = true)
    private ChainJpaEntity chain;

    @Column(name = "emission_factor", nullable = false)
    private BigDecimal emissionFactor;

    @Column(name = "co2_kg", nullable = false)
    private BigDecimal co2Kg;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false)
    private CalculationMethod calculationMethod;

    @Column(name = "cargo_weight_kg", precision = 19, scale = 4)
    private BigDecimal cargoWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_source")
    private EnergySource energySource;

    @Column(name = "consumption", precision = 19, scale = 4)
    private BigDecimal consumption;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;
}
