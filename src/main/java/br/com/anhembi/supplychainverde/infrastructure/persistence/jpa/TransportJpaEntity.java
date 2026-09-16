package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.FuelType;
import br.com.anhembi.supplychainverde.domain.enums.TransportMode;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transport")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transport_id")
    private Long transportId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chain_id", nullable = false, unique = true)
    private ChainJpaEntity chain;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false)
    private TransportMode transportMode;

    @Column(name = "distance", nullable = false)
    private BigDecimal distance;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(name = "capacity", nullable = false)
    private BigDecimal capacity;
}
