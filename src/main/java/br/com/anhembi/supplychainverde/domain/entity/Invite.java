package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;

/** Convite por email (feature 011). createdAt e expiresAt vêm do banco; expiresAt = createdAt + 24h. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invite {
    private Long inviteId;
    private Long invitedBy;
    private String invitedFor;
    private UserRole role;
    private String tokenHash;
    private boolean accepted;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public void accept() {
        if (accepted) throw new IllegalStateException("Convite já aceito.");
        this.accepted = true;
    }
}
