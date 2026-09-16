package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.StageType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chain")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChainJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chain_id")
    private Long chainId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private BatchJpaEntity batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_address_id")
    private AddressJpaEntity originAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_address_id")
    private AddressJpaEntity destinationAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_user_id", nullable = false)
    private UserJpaEntity responsibleUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false)
    private StageType stageType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
