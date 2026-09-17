package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    @Column(name = "emission_id")
    private Long emissionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chain_id", nullable = false, unique = true)
    private ChainJpaEntity chain;

    @Column(name = "emission_factor", nullable = false)
    private BigDecimal emissionFactor;

    @Column(name = "co2_kg", nullable = false)
    private BigDecimal co2Kg;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "calculation_method", nullable = false)
    private CalculationMethod calculationMethod;

    @Column(name = "calculated_at", nullable = false)
    private LocalDate calculatedAt;
}
