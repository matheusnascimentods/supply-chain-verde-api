package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Regras de nome e senha do próprio usuário, usadas por PATCH /users/me e PATCH /suppliers/me. Não grava nada. */
@Component
@RequiredArgsConstructor
public class OwnAccountChanges {
    private final PasswordHasher passwordHasher;

    // Separado de apply: o fornecedor valida empresa, endereço e usuário antes da primeira gravação.
    public void validate(User user, String userName, String currentPassword, String newPassword) {
        if (userName != null && userName.isBlank()) throw new ValidationException("Nome do usuário não pode ser vazio.");
        if (newPassword != null) {
            if (newPassword.isBlank()) throw new ValidationException("Nova senha não pode ser vazia.");
            if (currentPassword == null || !passwordHasher.matches(currentPassword, user.getPassword())) {
                throw new ValidationException("Senha atual incorreta.");
            }
        }
    }

    /** Aplica os campos enviados e diz se algo mudou, para o chamador decidir se grava. */
    public boolean apply(User user, String userName, String newPassword) {
        if (userName != null) user.setName(userName);
        if (newPassword != null) user.setPassword(passwordHasher.hash(newPassword));
        return userName != null || newPassword != null;
    }
}
