package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;

@Entity
@Table(name = "invite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long inviteId;

    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;

    @Column(name = "invited_for", nullable = false)
    private String invitedFor;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "accepted", nullable = false)
    private boolean accepted;

    // Preenchidas pelo banco (default e coluna gerada) e relidas depois do INSERT.
    @Generated
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Generated
    @Column(name = "expires_at", insertable = false, updatable = false)
    private LocalDateTime expiresAt;
}
