package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.EnergySource;
import br.com.anhembi.supplychainverde.domain.enums.FuelType;
import br.com.anhembi.supplychainverde.domain.enums.TransportMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "emission_factor")
@Getter
@NoArgsConstructor
public class EmissionFactorJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long emissionFactorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false)
    private CalculationMethod calculationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode")
    private TransportMode transportMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type")
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_source")
    private EnergySource energySource;

    @Column(name = "value", nullable = false, precision = 19, scale = 8)
    private BigDecimal value;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "source", nullable = false, length = 255)
    private String source;

    @Column(name = "reference_year", nullable = false)
    private Short referenceYear;
}
