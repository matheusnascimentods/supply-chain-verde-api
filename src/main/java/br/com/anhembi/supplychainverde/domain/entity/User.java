package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long userId;
    private String name;
    private String email;
    private String password;
    private UserRole role;
    private LocalDateTime createdAt;

    public void updateRole(UserRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("O novo perfil não pode ser nulo.");
        }
        this.role = newRole;
    }
}
